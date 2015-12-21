/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;

public class LogoutActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";

    private MaterialDialog mDialogLoggingOut;
    private AsyncTask<Void, Void, Void> mLogoutTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);
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

        SessionConnector.get().logout(LogoutActivity.this, new Runnable() {
            @Override
            public void run() {
                mDialogLoggingOut.dismiss();
                goToNextScreen();
            }
        });
    }

    private void goToNextScreen() {
        startActivity(new Intent(LogoutActivity.this, LoginActivity.class));
        finish();
    }
}
