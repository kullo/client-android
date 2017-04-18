/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.javautils.RuntimeAssertion;

import io.github.dialogsforandroid.MaterialDialog;

public class LeaveInboxActivity extends AppCompatActivity {
    private static final String TAG = "LeaveInboxActivity";

    private MaterialDialog mDialogLeavingInbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_leave_inbox);

        Ui.prepareActivityForTaskManager(this);

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDialogLeavingInbox = new MaterialDialog.Builder(this)
                .title(R.string.leaving_inbox_title)
                .content(R.string.leaving_inbox_description)
                .progress(true, 0)
                .cancelable(false)
                .show();

        KulloApplication.sharedInstance.startConversationParticipants.clear();

        SessionConnector.get().tryUnregisterPushTokenAsync(10000, new SessionConnector.UnregisterPushTokenCallback() {
            @Override
            public void onDone(boolean success) {
                if (!success) {
                    Log.w(TAG, "Push token could not be unregistered.");
                }

                SessionConnector.get().logout(LeaveInboxActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        mDialogLeavingInbox.dismiss();
                        goToNextScreen();
                    }
                });
            }
        });
    }

    @MainThread
    private void goToNextScreen() {
        startActivity(new Intent(LeaveInboxActivity.this, WelcomeActivity.class));
        finish();
    }
}
