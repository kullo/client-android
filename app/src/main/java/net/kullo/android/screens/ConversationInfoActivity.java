/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.util.adapters.AddressesAdapter;
import net.kullo.android.kulloapi.ConversationData;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.screens.conversationinfo.ParticipantViewHolder;
import net.kullo.android.ui.DividerDecoration;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConversationInfoActivity extends AppCompatActivity {
    private static final String TAG = ConversationInfoActivity.class.getSimpleName();

    private long mConversationId;
    private ConversationData mConversationData;

    @BindView(R.id.participants_list) RecyclerView recycler;
    @Nullable private ActionMode mActionMode;
    private AddressesAdapter<ParticipantViewHolder> mParticipantsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        Intent intent = getIntent();
        mConversationId = intent.getLongExtra(KulloConstants.CONVERSATION_ID, -1);

        setContentView(R.layout.activity_conversation_info);

        Ui.prepareActivityForTaskManager(this);
        Ui.setStatusBarColor(this, false, Ui.LayoutType.Other);
        Ui.setupActionbar(this, true);
        setTitle(R.string.conversation_info_title);

        ButterKnife.bind(this);

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        mConversationData = SessionConnector.get().getConversationData(this, mConversationId);
        setupParticipantsList();

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();
    }

    @Override
    protected void onStop() {
        stopActionMode();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupParticipantsList() {
        RuntimeAssertion.require(mConversationData != null);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recycler.setLayoutManager(llm);

        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        recycler.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));

        mParticipantsAdapter = new AddressesAdapter<ParticipantViewHolder>(mConversationData.participants) {
            @Override
            public ParticipantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.row_participant_full, parent, false);
                return new ParticipantViewHolder(parent.getContext(), itemView);
            }

            @SuppressWarnings("UnnecessaryLocalVariable")
            @Override
            public void onBindViewHolder(ParticipantViewHolder holder, int position) {
                final Address address = getItem(position);
                final String addressString = address.toString();
                final Bitmap avatar = mConversationData.participantsAvatar.get(addressString);
                final String organization = mConversationData.participantsOrganization.get(addressString);
                final String name = mConversationData.participantsName.get(addressString);

                String nameOrganization = name;
                if (!organization.isEmpty()) nameOrganization += " (" + organization + ")";
                holder.setValues(addressString, nameOrganization, avatar);
                holder.setSelected(isSelected(address));

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        toggleParticipantSelection(address);
                        return true;
                    }
                });
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mParticipantsAdapter.isSelectionActive()) {
                            toggleParticipantSelection(address);
                        } else {
                            DialogMaker.makeForKulloAddress(ConversationInfoActivity.this,
                                address, false).show();
                        }
                    }
                });
            }
        };
        recycler.setAdapter(mParticipantsAdapter);
    }

    private void toggleParticipantSelection(@NonNull final Address address) {
        mParticipantsAdapter.toggleSelectedItem(address);

        if (!mParticipantsAdapter.isSelectionActive()) {
            if (mActionMode != null) mActionMode.finish();
        } else {
            if (mActionMode == null) startParticipantsActionMode();
            final String title = String.format(
                getResources().getString(R.string.actionmode_title_n_selected),
                mParticipantsAdapter.getSelectedItemsCount());
            mActionMode.setTitle(title);
        }
    }

    private void startParticipantsActionMode() {
        Log.d(TAG, "Start actionMode");
        mActionMode = startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.actionmode_conversation_participants_list, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_start_conversation_with: {
                            AddressSet participants = new AddressSet(mParticipantsAdapter.getSelectedItems());
                            KulloApplication.sharedInstance.startConversationParticipants.addAll(participants);
                            startActivity(new Intent(ConversationInfoActivity.this, StartConversationActivity.class));
                            return true;
                        }
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mParticipantsAdapter.clearSelectedItems();
                    mActionMode = null;
                }
            });
    }

    public void stopActionMode() {
        if (mParticipantsAdapter != null && mActionMode != null) {
            mParticipantsAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }
}
