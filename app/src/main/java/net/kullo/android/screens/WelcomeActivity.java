/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import net.kullo.android.R;
import net.kullo.android.kulloapi.Credentials;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.LocalError;

import java.util.ArrayList;

import io.github.dialogsforandroid.MaterialDialog;

/**
 * Launching activity.
 *
 * Checks if credentials are stored and logs in using the {@link SessionConnector}.
 * After session is retrieved, {@link SessionConnector} calls Observers.
 *
 * Input fields are validated, errors are shown. With shown errors we need more space,
 * thus the header fades out if validation errors are shown.
 */
public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";

    private RelativeLayout mLayoutContent;
    private @Nullable MaterialDialog mCreatingSessionDialog;
    private @Nullable MaterialDialog mMigratingDialog;
    private ClientCreateSessionListenerObserver mClientCreateSessionListenerObserver;

    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Ui.prepareActivityForTaskManager(this);
        Ui.setStatusBarColor(this);

        mLayoutContent = (RelativeLayout) findViewById(R.id.content);
        connectButtons();

        // call registerListenerObservers() before checkForStoredCredentialsAndCreateSession()
        // to ensure existing login information cause an activity switch to ConversationsListActivity
        registerListenerObservers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Dialog googlePlayAvailabilityDialog = GcmConnector.get().checkGooglePlayAvailabilityAndPrompt(this);
        if (googlePlayAvailabilityDialog != null) {
            googlePlayAvailabilityDialog.show();
        } else {
            if (checkForStoredCredentialsAndCreateSession()) {
                // Activity started as an intermediate step to ConversationsListActivity (must be after logout)
                mLayoutContent.setVisibility(View.GONE);
            }
        }
    }

    // Avoid going back from Login to Main when there has been a logout
    // http://stackoverflow.com/questions/4190429/how-to-clear-the-android-stack-of-activities
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (mCreatingSessionDialog != null) mCreatingSessionDialog.dismiss();
        mCreatingSessionDialog = null;

        if (mMigratingDialog != null) mMigratingDialog.dismiss();
        mMigratingDialog = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying a welcome activity");
        unregisterObservers();
    }

    private void connectButtons() {
        findViewById(R.id.button_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                finish();
            }
        });

        findViewById(R.id.button_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, RegistrationActivity.class));
                finish();
            }
        });

        // Find stored addresses
        ArrayList<String> storedAddresses = new ArrayList<>();
        final SharedPreferences sharedPrefs = getSharedPreferences(KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE);
        for (String key : sharedPrefs.getAll().keySet()) {
            if (key.endsWith(KulloConstants.BLOCK_A)) {
                // extract address: -1 to account for the separator character
                storedAddresses.add(key.substring(0, key.length() - 1 - KulloConstants.BLOCK_A.length()));
            }
        }

        if (storedAddresses.isEmpty()) {
            findViewById(R.id.open_inbox_controls).setVisibility(View.GONE);
        } else {
            // populate the dropdown
            final Spinner spinnerOpenInbox = (Spinner)findViewById(R.id.spinner_open_inbox);
            ArrayAdapter<String> addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, storedAddresses);
            spinnerOpenInbox.setAdapter(addressAdapter);

            // select the one stored in last_address by default
            String lastActiveUser = sharedPrefs.getString(KulloConstants.LAST_ACTIVE_USER, "");
            if (!lastActiveUser.isEmpty()) {
                int position = storedAddresses.indexOf(lastActiveUser);
                spinnerOpenInbox.setSelection(position);
            }

            // let the button jump to login while having set this as active... it should log in automatically... or even call myself?
            findViewById(R.id.button_open_inbox).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String selectedAddress = spinnerOpenInbox.getSelectedItem().toString();
                    sharedPrefs.edit().putString(KulloConstants.ACTIVE_USER, selectedAddress).apply();
                    if (checkForStoredCredentialsAndCreateSession()) {
                        sharedPrefs.edit().putString(KulloConstants.LAST_ACTIVE_USER, selectedAddress).apply();
                        mLayoutContent.setVisibility(View.GONE);
                    }
                }
            });
        }

    }

    //API

    private void registerListenerObservers() {
        mClientCreateSessionListenerObserver = new ClientCreateSessionListenerObserver() {
            @Override
            public void migrationStarted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) mCreatingSessionDialog.dismiss();
                        mCreatingSessionDialog = null;

                        mMigratingDialog = new MaterialDialog.Builder(WelcomeActivity.this)
                                .title(R.string.create_session_progress_login_title)
                                .content(R.string.create_session_migrating)
                                .progress(true, 0)
                                .cancelable(false)
                                .show();
                    }
                });
            }

            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(WelcomeActivity.this, ConversationsListActivity.class));
                        finish();
                    }
                });
            }

            @Override
            public void error(final LocalError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) mCreatingSessionDialog.dismiss();
                        mCreatingSessionDialog = null;

                        DialogMaker.makeForLocalError(WelcomeActivity.this, error).show();
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }

    private void unregisterObservers() {
        RuntimeAssertion.require(mClientCreateSessionListenerObserver != null);
        SessionConnector.get().removeListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }

    //HELPERS

    private boolean checkForStoredCredentialsAndCreateSession() {
        Credentials credentials = SessionConnector.get().loadStoredCredentials(this);
        if (credentials == null) {
            Log.d(TAG, "No stored Kullo user settings found");
            return false;
        }

        Log.d(TAG, "Stored Kullo address: " + credentials.getAddress().toString());

        mCreatingSessionDialog = new MaterialDialog.Builder(this)
                .title(R.string.create_session_progress_login_title)
                .content(R.string.create_session_please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();

        SessionConnector.get().createSession(this, credentials);
        return true;
    }
}
