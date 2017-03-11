/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.ClientConnector;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AddressAutocompleteAdapter;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.screens.startconversation.ParticipantsAdapter;
import net.kullo.android.ui.NonScrollingLinearLayoutManager;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AsyncTask;
import net.kullo.libkullo.api.ClientAddressExistsListener;
import net.kullo.libkullo.api.NetworkError;

import io.github.dialogsforandroid.MaterialDialog;

public class StartConversationActivity extends AppCompatActivity {
    @SuppressWarnings("unused") private static final String TAG = "StartConversationAct."; // max. 23 chars

    private TextInputLayout mNewParticipantTextInputLayout;
    private AutoCompleteTextView mNewParticipantEditText;
    private ParticipantsAdapter mParticipantsAdapter;
    private TextView mParticipantsHeader;
    private MaterialDialog mWaitingDialog;
    private AsyncTask mAddrExistsTask;
    private boolean mReadyToLeave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_start_conversation);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);
        setTitle(getResources().getString(R.string.new_conversation));

        setupLayout();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);
    }

    @Override
    public void onResume(){
        super.onResume();

        GcmConnector.get().removeAllNotifications(this);

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
        RuntimeAssertion.require(mNewParticipantTextInputLayout != null);
        mNewParticipantEditText = (AutoCompleteTextView) mNewParticipantTextInputLayout.getEditText();
        RuntimeAssertion.require(mNewParticipantEditText != null);
        mNewParticipantEditText.addTextChangedListener(KulloConstants.KULLO_ADDRESS_AT_THIEF);
        mNewParticipantEditText.setAdapter(new AddressAutocompleteAdapter(this));

        // add button
        View buttonAdd = findViewById(R.id.button_add_participant);
        RuntimeAssertion.require(buttonAdd != null);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addParticipant();
            }
        });

        // keyboard action
        mNewParticipantEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addParticipant();
                return true;
            }
        });

        // list header
        mParticipantsHeader = (TextView) findViewById(R.id.participants_header);
        RuntimeAssertion.require(mParticipantsHeader != null);
        mParticipantsHeader.setVisibility(View.INVISIBLE);

        // participants list
        mParticipantsAdapter = new ParticipantsAdapter();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.participants_list);
        RuntimeAssertion.require(recyclerView != null);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new NonScrollingLinearLayoutManager(this));
        recyclerView.setAdapter(mParticipantsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_new_conversation:
                String newParticipantAddress = mNewParticipantEditText.getText().toString();
                if (newParticipantAddress.isEmpty()) {
                    proceedLeave();
                } else {
                    mReadyToLeave = true;
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
        if (mParticipantsAdapter.getItemCount() > 0) {
            long conversationId = SessionConnector.get().addNewConversationForKulloAddresses(mParticipantsAdapter.getItems());

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
        String newParticipantAddress = mNewParticipantEditText.getText().toString();

        // validation
        mNewParticipantTextInputLayout.setError(null);

        // prevent conversation with self
        if (newParticipantAddress.equals(SessionConnector.get().getCurrentUserAddressAsString())) {
            mNewParticipantTextInputLayout.setError(getResources().getText(R.string.sender_is_recipient));
            return;
        }

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

            mAddrExistsTask = ClientConnector.get().getClient().addressExistsAsync(
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
                                        mNewParticipantEditText.setText("");
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
