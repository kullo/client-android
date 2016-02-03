/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.UserSettings;

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
    private MaterialDialog mCreatingSessionDialog;
    private ClientCreateSessionListenerObserver mClientCreateSessionListenerObserver;

    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mLayoutContent = (RelativeLayout) findViewById(R.id.content);

        // Check google play
        GcmConnector.get().checkGooglePlayAndPrompt(this);

        // call registerCreateSessionListenerObserver before checkForStoredCredentialsAndCreateSession
        // to ensure existing login information cause an activity switch to ConversationsListActivity
        registerCreateSessionListenerObserver();

        if (checkForStoredCredentialsAndCreateSession()) {
            // Activity started as an intermediate step to ConversationsListActivity (must be after logout)
            mLayoutContent.setVisibility(View.GONE);
        } else {
            // Activity started for user to choose login or register
            Ui.setColorStatusBarArrangeHeader(this);
            connectButtons();
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

        if (mCreatingSessionDialog != null) {
            mCreatingSessionDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying a login activity");
        unregisterCreateSessionListenerObserver();
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
    }

    //API

    private void registerCreateSessionListenerObserver() {
        mClientCreateSessionListenerObserver = new ClientCreateSessionListenerObserver() {
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
            public void loginFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) {
                            mCreatingSessionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    new MaterialDialog.Builder(WelcomeActivity.this)
                                            .title(R.string.error_login_failed_title)
                                            .content(R.string.error_login_failed_description)
                                            .neutralText(R.string.ok)
                                            .cancelable(false)
                                            .show();
                                }});

                            mCreatingSessionDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void networkError(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) {
                            mCreatingSessionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    DialogMaker.makeForNetworkError(WelcomeActivity.this, error).show();
                                }});
                            mCreatingSessionDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void localError(final LocalError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) {
                            mCreatingSessionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    DialogMaker.makeForLocalError(WelcomeActivity.this, error).show();
                                }});
                            mCreatingSessionDialog.dismiss();
                        }
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }

    private void unregisterCreateSessionListenerObserver() {
        RuntimeAssertion.require(mClientCreateSessionListenerObserver != null);
        SessionConnector.get().removeListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }

    //HELPERS

    private boolean checkForStoredCredentialsAndCreateSession() {
        UserSettings us = SessionConnector.get().loadStoredUserSettings(this);

        if (us == null) {
            Log.d(TAG, "No stored Kullo user settings found");
            return false;
        }

        Log.d(TAG, "Stored Kullo address: " + us.address().toString());

        //show waiting dialog
        mCreatingSessionDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_login)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();

        SessionConnector.get().createSession(this, us);
        return true;
    }
}
