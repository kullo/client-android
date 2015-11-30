/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.screens.startconversation.ParticipantsAdapter;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AsyncTask;
import net.kullo.libkullo.api.ClientAddressExistsListener;
import net.kullo.libkullo.api.NetworkError;


public class StartConversationActivity extends AppCompatActivity {
    private static final String TAG = "StartConversationAct."; // max. 23 chars

    private TextInputLayout mNewParticipantTextInputLayout;
    private EditText mNewParticipantEditText;
    private RecyclerView mRecyclerView;
    private ParticipantsAdapter mParticipantsAdapter;
    private TextView mParticipantsHeader;
    private MaterialDialog mWaitingDialog;
    private AsyncTask mAddrExistsTask;
    private boolean mReadyToLeave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_start_conversation);

        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);
        setTitle(getResources().getString(R.string.new_conversation));

        setupLayout();

        if (task != null) task.waitUntilDone();
    }

    @Override
    public void onResume(){
        super.onResume();

        mReadyToLeave = false;
    }

    @Override
    public void onDestroy() {
        if (mAddrExistsTask != null) mAddrExistsTask.cancel();

        super.onDestroy();
    }

    private void setupLayout() {
        // text field
        mNewParticipantTextInputLayout = (TextInputLayout) findViewById(R.id.new_participant_text_input_layout);
        mNewParticipantEditText = mNewParticipantTextInputLayout.getEditText();
        RuntimeAssertion.require(mNewParticipantEditText != null);
        mNewParticipantEditText.addTextChangedListener(KulloConstants.KULLO_ADDRESS_AT_THIEF);

        // add button
        findViewById(R.id.button_add_participant).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addParticipant();
            }
        });

        // list header
        mParticipantsHeader = (TextView) findViewById(R.id.participantsHeader);
        mParticipantsHeader.setVisibility(View.INVISIBLE);

        // participants list
        mParticipantsAdapter = new ParticipantsAdapter();

        mRecyclerView = (RecyclerView) findViewById(R.id.participantsList);
        LinearLayoutManager llm = new org.solovyev.android.views.llm.LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(mParticipantsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_new_conversation:
                String newParticipantAddress = mNewParticipantTextInputLayout.getEditText().getText().toString();
                if (newParticipantAddress.isEmpty()) {
                    proceedLeave();
                } else {
                    mReadyToLeave = true;
                    addParticipant();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_start_conversation, menu);
        return true;
    }

    private void proceedLeave() {
        if (mParticipantsAdapter.getItemCount() > 0) {
            long conversationId = KulloConnector.get().addNewConversationForKulloAddresses(mParticipantsAdapter.getItems());

            Intent intent = new Intent(this, MessagesListActivity.class);
            intent.putExtra(MessagesListActivity.CONVERSATION_ID, conversationId);
            startActivity(intent);
            // remove itself from the activity stack
            finish();
        } else {
            if (mWaitingDialog != null) {
                mWaitingDialog.dismiss();
            }
            new MaterialDialog.Builder(StartConversationActivity.this)
                .title(R.string.create_conversation_failed)
                .content(R.string.participant_needed)
                .neutralText(R.string.ok)
                .cancelable(false)
                .show();
        }
    }

    private void addParticipant() {
        String newParticipantAddress = mNewParticipantTextInputLayout.getEditText().getText().toString();

        //validation
        mNewParticipantTextInputLayout.setError(null);
        Address newParticipant = Address.create(newParticipantAddress);

        if (newParticipant == null) {
            //validation failed
            mNewParticipantTextInputLayout.setError(getResources().getText(R.string.login_error_address_invalid));

        } else {
            //show waiting dialog
            mWaitingDialog = new MaterialDialog.Builder(this)
                    .title(R.string.progress_new_participant)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            mAddrExistsTask = KulloConnector.get().getClient().addressExistsAsync(
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
                                        mParticipantsAdapter.notifyDataSetChanged();
                                        mNewParticipantTextInputLayout.getEditText().setText("");
                                        mParticipantsHeader.setVisibility(View.VISIBLE);
                                        if (mReadyToLeave) {
                                            proceedLeave();
                                        }
                                    } else {
                                        mNewParticipantTextInputLayout.setError(getResources().getText(R.string.address_not_exist));
                                    }
                                    mReadyToLeave = false;
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
                                                        .title(R.string.address_check_failed_error_header)
                                                        .content(R.string.address_check_failed_connection_error)
                                                        .neutralText(R.string.ok)
                                                        .cancelable(false)
                                                        .show();
                                            }});

                                        mWaitingDialog.dismiss();
                                    }
                                    mReadyToLeave = false;
                                }
                            });
                        }
                    });
        }
    }

}
