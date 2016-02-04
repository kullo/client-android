/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.libkullo.api.AsyncTask;

public class LogoutActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";

    private MaterialDialog mDialogLoggingOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = SessionConnector.get().createActivityWithSession(this);
        setContentView(R.layout.activity_logout);
        if (task != null) task.waitUntilDone();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDialogLoggingOut = new MaterialDialog.Builder(this)
                .title(R.string.logging_out)
                .content(R.string.logging_out_description)
                .progress(true, 0)
                .cancelable(false)
                .show();

        SessionConnector.get().unregisterPushToken();
        SessionConnector.get().logout(LogoutActivity.this, new Runnable() {
            @Override
            public void run() {
                mDialogLoggingOut.dismiss();
                goToNextScreen();
            }
        });
    }

    private void goToNextScreen() {
        startActivity(new Intent(LogoutActivity.this, WelcomeActivity.class));
        finish();
    }
}
