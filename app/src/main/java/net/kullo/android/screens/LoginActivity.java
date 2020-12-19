/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AddressAutocompleteAdapter;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.observers.listenerobservers.ClientCheckCredentialsListenerObserver;
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AddressHelpers;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.MasterKeyHelpers;
import net.kullo.libkullo.api.NetworkError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.dialogsforandroid.MaterialDialog;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private TextInputLayout mKulloAddressTextInputLayout;
    private EditText mKulloAddressEditText;
    private List<TextInputLayout> mMasterKeyBlocksTextInputLayout;
    private List<EditText> mMasterKeyBlocksEditText;
    private @Nullable MaterialDialog mCreatingSessionDialog;
    private @Nullable MaterialDialog mMigratingDialog;
    private ClientCreateSessionListenerObserver mClientCreateSessionListenerObserver;
    private ClientCheckCredentialsListenerObserver mClientCheckCredentialsListenerObserver;

    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating a login activity");

        clearObsoleteLoginActivityPreferences();

        setContentView(R.layout.activity_login);

        Ui.prepareActivityForTaskManager(this);
        setupLayout();

        // Activity started for user to type in MasterKey

        registerListenerObservers();
        Ui.setStatusBarColor(this);
        connectLayout();
    }

    private void clearObsoleteLoginActivityPreferences() {
        // Those preferences (no name set, stored in screens.LoginActivity.xml)
        // were needed when android:noHistory was "true" for LoginActivity.
        // Android default is that we automatically restore activity, scroll position
        // text input and focused input field.
        SharedPreferences.Editor preferencesEditor = getPreferences(MODE_PRIVATE).edit();
        preferencesEditor.clear().apply();
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
        Log.d(TAG, "Destroying a login activity");
        unregisterListenerObservers();
    }

    @Override
    public void onBackPressed() {
        // we kill this activity and recreate Welcome when pressing back
        // because the two listenerobservers are tied to the create-destroy state changes of these activities
        startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
        finish();
    }

    //LAYOUT
    private void setupLayout() {
        mKulloAddressTextInputLayout = (TextInputLayout) findViewById(R.id.edit_kullo_address);
        mMasterKeyBlocksTextInputLayout = new ArrayList<>(Arrays.asList(
                (TextInputLayout) findViewById(R.id.edit_block_a),
                (TextInputLayout) findViewById(R.id.edit_block_b),
                (TextInputLayout) findViewById(R.id.edit_block_c),
                (TextInputLayout) findViewById(R.id.edit_block_d),
                (TextInputLayout) findViewById(R.id.edit_block_e),
                (TextInputLayout) findViewById(R.id.edit_block_f),
                (TextInputLayout) findViewById(R.id.edit_block_g),
                (TextInputLayout) findViewById(R.id.edit_block_h),
                (TextInputLayout) findViewById(R.id.edit_block_i),
                (TextInputLayout) findViewById(R.id.edit_block_j),
                (TextInputLayout) findViewById(R.id.edit_block_k),
                (TextInputLayout) findViewById(R.id.edit_block_l),
                (TextInputLayout) findViewById(R.id.edit_block_m),
                (TextInputLayout) findViewById(R.id.edit_block_n),
                (TextInputLayout) findViewById(R.id.edit_block_o),
                (TextInputLayout) findViewById(R.id.edit_block_p)
        ));

        // Setup EditTexts
        mKulloAddressEditText = mKulloAddressTextInputLayout.getEditText();
        mMasterKeyBlocksEditText = new ArrayList<>(Arrays.asList(
                mMasterKeyBlocksTextInputLayout.get( 0).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 1).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 2).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 3).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 4).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 5).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 6).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 7).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 8).getEditText(),
                mMasterKeyBlocksTextInputLayout.get( 9).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(10).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(11).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(12).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(13).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(14).getEditText(),
                mMasterKeyBlocksTextInputLayout.get(15).getEditText()
        ));

        // getEditText() is nullable. Make sure the editTexts are not null
        RuntimeAssertion.require(mKulloAddressEditText != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 0) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 1) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 2) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 3) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 4) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 5) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 6) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 7) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 8) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get( 9) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(10) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(11) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(12) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(13) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(14) != null);
        RuntimeAssertion.require(mMasterKeyBlocksEditText.get(15) != null);
    }

    private void connectLayout() {
        connectButtons();
        connectTextInputFields();
        configureEditFieldsToSwitchToNextIfFilled();
        setupAddressAutocompletion();
    }

    private void connectButtons() {
        findViewById(R.id.button_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginClicked(v);
            }
        });
        ((EditText) findViewById(R.id.edit_block_p_text)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loginClicked(v);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void connectTextInputFields() {
        mKulloAddressEditText.addTextChangedListener(KulloConstants.KULLO_ADDRESS_AT_THIEF);
    }

    private void configureEditFieldsToSwitchToNextIfFilled() {
        for (int i = 0 ; i < mMasterKeyBlocksTextInputLayout.size(); i++) {
            final TextInputLayout currentLayout = mMasterKeyBlocksTextInputLayout.get(i);
            final EditText current = mMasterKeyBlocksEditText.get(i);
            // When current is the last block, next is null.
            final EditText next = (i+1 > mMasterKeyBlocksTextInputLayout.size()-1) ? null
                    : mMasterKeyBlocksTextInputLayout.get(i+1).getEditText();

            current.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) { }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                public void afterTextChanged(Editable s) {
                    if(liveValidateInput(currentLayout)) {
                        if (next != null) {
                            next.requestFocus();
                        }
                    }
                }
            });
        }
    }

    private void setupAddressAutocompletion() {
        AutoCompleteTextView textView = (AutoCompleteTextView) mKulloAddressTextInputLayout.getEditText();
        RuntimeAssertion.require(textView != null);
        textView.setAdapter(new AddressAutocompleteAdapter(this));
    }

    private boolean liveValidateInput(TextInputLayout textInputLayout) {
        EditText editText = textInputLayout.getEditText();
        RuntimeAssertion.require(editText != null);
        String text = editText.getText().toString();

        if (text.length() == KulloConstants.BLOCK_SIZE) {
            if (KulloUtils.isValidMasterKeyBlock(text)) {
                textInputLayout.setError(null);
                textInputLayout.setErrorEnabled(false);
                return true;
            } else {
                textInputLayout.setError(getResources().getText(R.string.login_error_masterkey_block_invalid));
                textInputLayout.setErrorEnabled(true);
                return false;
            }
        } else {
            // not done typing yet
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
            return false;
        }
    }

    //LAYOUT ACTIONS

    public void loginClicked(View v) {
        int validationFailures = 0;

        // reset error status
        mKulloAddressTextInputLayout.setError(null);
        mKulloAddressTextInputLayout.setErrorEnabled(false);
        for (TextInputLayout blockText : mMasterKeyBlocksTextInputLayout) {
            blockText.setError(null);
            blockText.setErrorEnabled(false);
        }

        // validate input fields
        if (!KulloUtils.isValidKulloAddress(mKulloAddressEditText.getText().toString())) {
            mKulloAddressTextInputLayout.setError(getResources().getText(R.string.login_error_address_invalid));
            mKulloAddressTextInputLayout.setErrorEnabled(true);
            validationFailures += 1;
        }
        for (TextInputLayout blockText : mMasterKeyBlocksTextInputLayout) {
            validationFailures += blockIsValidIfNotSetError(blockText);
        }

        if (validationFailures == 0) {
            // Gather data
            final String addressString = mKulloAddressEditText.getText().toString();
            final Address address = AddressHelpers.create(addressString);
            final ArrayList<String> masterKeyBlocks = new ArrayList<>(16);
            for (EditText block : mMasterKeyBlocksEditText) {
                masterKeyBlocks.add(block.getText().toString());
            }
            final MasterKey masterKey = MasterKeyHelpers.createFromDataBlocks(masterKeyBlocks);

            // Show waiting dialog (e.g. for long database migration)
            mCreatingSessionDialog = new MaterialDialog.Builder(this)
                    .title(R.string.create_session_progress_login_title)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            SessionConnector.get().checkLoginAndCreateSession(this, address, masterKey);
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

                        mMigratingDialog = new MaterialDialog.Builder(LoginActivity.this)
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

                        startActivity(new Intent(LoginActivity.this, ConversationsListActivity.class));
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

                        DialogMaker.makeForLocalError(LoginActivity.this, error).show();
                    }
                });
            }
        };

        mClientCheckCredentialsListenerObserver = new ClientCheckCredentialsListenerObserver() {
            @Override
            public void loginFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) mCreatingSessionDialog.dismiss();
                        mCreatingSessionDialog = null;

                        new MaterialDialog.Builder(LoginActivity.this)
                                .title(R.string.login_error_failed_title)
                                .content(R.string.login_error_failed_description)
                                .neutralText(R.string.ok)
                                .cancelable(false)
                                .show();
                    }
                });
            }

            @Override
            public void error(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCreatingSessionDialog != null) mCreatingSessionDialog.dismiss();
                        mCreatingSessionDialog = null;

                        DialogMaker.makeForNetworkError(LoginActivity.this, error).show();
                    }
                });
            }
        };

        SessionConnector.get().addListenerObserver(
                ClientCheckCredentialsListenerObserver.class,
                mClientCheckCredentialsListenerObserver);
        SessionConnector.get().addListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }

    private void unregisterListenerObservers() {
        RuntimeAssertion.require(mClientCheckCredentialsListenerObserver != null);
        RuntimeAssertion.require(mClientCreateSessionListenerObserver != null);
        SessionConnector.get().removeListenerObserver(
                ClientCheckCredentialsListenerObserver.class,
                mClientCheckCredentialsListenerObserver);
        SessionConnector.get().removeListenerObserver(
                ClientCreateSessionListenerObserver.class,
                mClientCreateSessionListenerObserver);
    }


    // validating MasterKey blocks
    private int blockIsValidIfNotSetError(TextInputLayout textInputLayout) {
        EditText editText = textInputLayout.getEditText();
        RuntimeAssertion.require(editText != null);
        String text = editText.getText().toString();
        if (!KulloUtils.isValidMasterKeyBlock(text)) {
            textInputLayout.setError(getResources().getText(R.string.login_error_masterkey_block_invalid));
            textInputLayout.setErrorEnabled(true);
            return 1;
        } else {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
            return 0;
        }
    }
}
