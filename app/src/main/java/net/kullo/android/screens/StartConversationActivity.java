/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.ClientConnector;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AddressAutocompleteAdapter;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.screens.startconversation.ParticipantsAdapter;
import net.kullo.android.ui.NonScrollingLinearLayoutManager;
import net.kullo.android.util.adapters.RecyclerItemClickListener;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AsyncTask;
import net.kullo.libkullo.api.ClientAddressExistsListener;
import net.kullo.libkullo.api.NetworkError;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.dialogsforandroid.MaterialDialog;

public class StartConversationActivity extends KulloActivity {
    @SuppressWarnings("unused") private static final String TAG = "StartConversationAct."; // max. 23 chars

    @BindView(R.id.new_participant_text_input_layout) TextInputLayout mNewParticipantTextInputLayout;
    @BindView(R.id.new_participant) AutoCompleteTextView mNewParticipantEditText;
    @BindView(R.id.button_add_participant) View mButtonAdd;
    @BindView(R.id.participants_header) TextView mParticipantsHeader;
    @BindView(R.id.participants_list) RecyclerView mParticipantsList;

    @Nullable private MaterialDialog mWaitingDialog;
    @NonNull final private ParticipantsAdapter mParticipantsAdapter = new ParticipantsAdapter(this);
    @Nullable private AsyncTask mAddressExistsTask;
    private boolean mUserRequestedToStartConversation;
    @Nullable private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_start_conversation);

        ButterKnife.bind(this);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);
        setTitle(getResources().getString(R.string.start_conversation_new_conversation));

        setupLayout();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        fillAdapterWithInitialData();
        updateViewFromAdapterData();

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();
    }

    @Override
    public void onResume(){
        super.onResume();

        GcmConnector.get().removeAllNotifications(this);

        mUserRequestedToStartConversation = false;
    }

    @Override
    protected void onStop() {
        stopActionMode();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mAddressExistsTask != null) mAddressExistsTask.cancel();

        super.onDestroy();
    }

    private void setupLayout() {
        // text field
        mNewParticipantEditText.addTextChangedListener(KulloConstants.KULLO_ADDRESS_AT_THIEF);
        mNewParticipantEditText.setAdapter(new AddressAutocompleteAdapter(this));
        mNewParticipantEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addParticipant();
                return true;
            }
        });

        // add button
        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addParticipant();
            }
        });

        // participants list
        mParticipantsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                saveAdapterContentToApp();
                updateViewFromAdapterData();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                saveAdapterContentToApp();
                updateViewFromAdapterData();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                saveAdapterContentToApp();
                updateViewFromAdapterData();
            }
        });
        mParticipantsAdapter.setOnClickListener(new RecyclerItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                if (mActionMode != null) {
                    Address participant = mParticipantsAdapter.getItem(position);
                    toggleParticipantSelection(participant);
                }
            }

            @Override
            public boolean onLongClick(View v, int position) {
                Address participant = mParticipantsAdapter.getItem(position);
                toggleParticipantSelection(participant);
                return false;
            }
        });

        mParticipantsList.setNestedScrollingEnabled(false);
        mParticipantsList.setLayoutManager(new NonScrollingLinearLayoutManager(this));
        mParticipantsList.setAdapter(mParticipantsAdapter);
    }

    private void toggleParticipantSelection(final Address participant) {
        mParticipantsAdapter.toggleSelectedItem(participant);

        if (!mParticipantsAdapter.isSelectionActive()) {
            if (mActionMode != null) mActionMode.finish();
        } else {
            if (mActionMode == null) setupParticipantsActionMode();

            // update action bar title
            final String title = String.format(
                getResources().getString(R.string.actionmode_title_n_selected),
                mParticipantsAdapter.getSelectedItemsCount());
            mActionMode.setTitle(title);
        }
    }

    private void setupParticipantsActionMode() {
        mActionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.actionmode_start_conversation_participants_list, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_remove: {
                        while (mParticipantsAdapter.getSelectedItemsCount() > 0) {
                            // Remove arbitrary item from selection
                            Address participant = mParticipantsAdapter.getSelectedItems().iterator().next();
                            mParticipantsAdapter.remove(participant);
                        }
                        mode.finish();
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
        if (mActionMode != null) {
            mParticipantsAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }

    private void fillAdapterWithInitialData() {
        for (final Address address : KulloApplication.sharedInstance.startConversationParticipants) {
            mParticipantsAdapter.add(address);
        }
    }

    private void updateViewFromAdapterData() {
        // list header
        if (mParticipantsAdapter.getItemCount() == 0) {
            mParticipantsHeader.setVisibility(View.GONE);
        } else {
            mParticipantsHeader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_conversation:
                String newParticipantAddress = mNewParticipantEditText.getText().toString();
                if (newParticipantAddress.isEmpty()) {
                    proceedLeave();
                } else {
                    mUserRequestedToStartConversation = true;
                    addParticipant();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_start_conversation, menu);
        return true;
    }

    private void proceedLeave() {
        final AddressSet addressesToBeUsed = mParticipantsAdapter.getItems();
        if (addressesToBeUsed.size() > 0) {
            long conversationId = SessionConnector.get().addNewConversationForKulloAddresses(addressesToBeUsed);

            Intent intent = new Intent(this, MessagesListActivity.class);
            intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
            startActivity(intent);

            // Clear participant list storage
            KulloApplication.sharedInstance.startConversationParticipants.clear();

            // remove itself from the activity stack
            finish();
        } else {
            if (mWaitingDialog != null) {
                mWaitingDialog.dismiss();
            }
            new MaterialDialog.Builder(StartConversationActivity.this)
                .title(R.string.start_conversation_create_conversation_failed)
                .content(R.string.start_conversation_participant_needed)
                .neutralText(R.string.ok)
                .cancelable(false)
                .show();
        }
    }

    private void addParticipant() {
        Address newParticipant = Address.create(mNewParticipantEditText.getText().toString());

        if (newParticipant == null) {
            mNewParticipantTextInputLayout.setError(getText(R.string.login_error_address_invalid));
        } else if (newParticipant.isEqualTo(SessionConnector.get().getCurrentUserAddress())) {
            // prevent conversation with self
            mNewParticipantTextInputLayout.setError(getText(R.string.start_conversation_sender_is_recipient));
        } else {
            mNewParticipantTextInputLayout.setError(null);

            //show waiting dialog
            mWaitingDialog = new MaterialDialog.Builder(this)
                    .title(R.string.start_conversation_progress_new_participant)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            mAddressExistsTask = ClientConnector.get().getClient().addressExistsAsync(
                    newParticipant,
                    new ClientAddressExistsListener() {

                        @Override
                        public void finished(final Address address, final boolean exists) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mWaitingDialog != null) {
                                        mWaitingDialog.dismiss();
                                    }

                                    if (exists) {
                                        mParticipantsAdapter.add(address);

                                        mNewParticipantEditText.setText("");
                                        if (mUserRequestedToStartConversation) {
                                            proceedLeave();
                                        }
                                    } else {
                                        mNewParticipantTextInputLayout.setError(getResources().getText(R.string.start_conversation_address_not_exist));
                                    }
                                    mUserRequestedToStartConversation = false;
                                }
                            });
                        }

                        @Override
                        public void error(Address address, NetworkError error) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mWaitingDialog != null) {
                                        mWaitingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                            public void onDismiss(DialogInterface dialog) {
                                                new MaterialDialog.Builder(StartConversationActivity.this)
                                                        .title(R.string.start_conversation_address_check_failed_error_header)
                                                        .content(R.string.start_conversation_address_check_failed_connection_error)
                                                        .neutralText(R.string.ok)
                                                        .cancelable(false)
                                                        .show();
                                            }});

                                        mWaitingDialog.dismiss();
                                    }
                                    mUserRequestedToStartConversation = false;
                                }
                            });
                        }
                    });
        }
    }

    private void saveAdapterContentToApp() {
        KulloApplication.sharedInstance.startConversationParticipants = mParticipantsAdapter.getItems();
    }
}
