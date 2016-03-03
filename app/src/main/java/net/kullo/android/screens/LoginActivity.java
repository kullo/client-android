/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.littlehelpers.AddressAutocompleteAdapter;
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.NetworkError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launching activity.
 *
 * Checks if credentials are stored and logs in using the {@link SessionConnector}.
 * After session is retrieved, {@link SessionConnector} calls Observers.
 *
 * Input fields are validated, errors are shown. With shown errors we need more space,
 * thus the header fades out if validation errors are shown.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private RelativeLayout mLayoutContent;
    private TextInputLayout mKulloAddressTextInputLayout;
    private EditText mKulloAddressEditText;
    private List<TextInputLayout> mMasterKeyBlocksTextInputLayout;
    private List<EditText> mMasterKeyBlocksEditText;
    private MaterialDialog mCreatingSessionDialog;
    private ClientCreateSessionListenerObserver mClientCreateSessionListenerObserver;
    private boolean mPreserveStatus;

    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating a login activity");
        setContentView(R.layout.activity_login);

        Ui.prepareActivityForTaskManager(this);
        setupLayout();

        // Activity started for user to type in MasterKey

        registerCreateSessionListenerObserver();
        Ui.setColorStatusBarArrangeHeader(this);
        mPreserveStatus = true;
        connectLayout();
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String storedAddress = preferences.getString(KulloConstants.ACTIVE_USER, "");
        if (!storedAddress.isEmpty()) {
            mKulloAddressEditText.setText(storedAddress);
        }

        for (int i = 0; i < mMasterKeyBlocksEditText.size(); i++) {
            String storedBlock = preferences.getString("block_" + i, "");
            if (!storedBlock.isEmpty()) {
                mMasterKeyBlocksEditText.get(i).setText(storedBlock);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences.Editor preferencesEditor = getPreferences(MODE_PRIVATE).edit();

        if (mPreserveStatus) {
            preferencesEditor.putString(KulloConstants.ACTIVE_USER, mKulloAddressEditText.getText().toString());

            for (int i = 0 ; i < mMasterKeyBlocksEditText.size() ; i++) {
                preferencesEditor.putString("block_" + i, mMasterKeyBlocksEditText.get(i).getText().toString());
            }

            preferencesEditor.apply();
        } else {
            preferencesEditor.clear().apply();
        }
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

    @Override
    public void onBackPressed() {
        // we kill this activity and recreate Welcome when pressing back
        // because the two listenerobservers are tied to the create-destroy state changes of these activities
        startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
        finish();
    }

    //LAYOUT
    private void setupLayout() {
        mLayoutContent = (RelativeLayout) findViewById(R.id.content);
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
        setDomainAutocompletion();
    }

    private void connectButtons() {
        findViewById(R.id.button_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginClicked(v);
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

    private void setDomainAutocompletion() {
        AutoCompleteTextView textView = (AutoCompleteTextView)mKulloAddressTextInputLayout.getEditText();
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
            final Address address = Address.create(addressString);
            final ArrayList<String> masterKeyBlocks = new ArrayList<>(16);
            for (EditText block : mMasterKeyBlocksEditText) {
                masterKeyBlocks.add(block.getText().toString());
            }
            final MasterKey masterKey = MasterKey.createFromDataBlocks(masterKeyBlocks);

            // Show waiting dialog (e.g. for long database migration)
            mCreatingSessionDialog = new MaterialDialog.Builder(this)
                    .title(R.string.progress_login)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            SessionConnector.get().checkLoginAndCreateSession(this, address, masterKey);
        }
    }

    //API

    private void registerCreateSessionListenerObserver() {
        mClientCreateSessionListenerObserver = new ClientCreateSessionListenerObserver() {
            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreserveStatus = false;

                        startActivity(new Intent(LoginActivity.this, ConversationsListActivity.class));
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
                                    new MaterialDialog.Builder(LoginActivity.this)
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
                                    DialogMaker.makeForNetworkError(LoginActivity.this, error).show();
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
                                    DialogMaker.makeForLocalError(LoginActivity.this, error).show();
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
