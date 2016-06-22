/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.registration;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.kullo.android.R;
import net.kullo.android.kulloapi.Credentials;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.observers.listenerobservers.RegistrationRegisterAccountListenerObserver;
import net.kullo.android.screens.RegistrationActivity;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AddressNotAvailableReason;
import net.kullo.libkullo.api.Challenge;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.NetworkError;

public class ChooseAddressFragment extends Fragment {
    private static final String TAG = "ChooseAddressFragment";

    private View mFragmentRoot;
    private RegistrationRegisterAccountListenerObserver mRegistrationRegisterAccountListenerObserver = null;
    private Button mRegisterButton;
    private TextInputLayout mAddressInputLayout;
    private EditText mAddressEditText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration_choose_address, container, false);

        mFragmentRoot = view.findViewById(R.id.fragment_root);
        mAddressInputLayout = (TextInputLayout) view.findViewById(R.id.edit_kullo_address);
        mAddressEditText = mAddressInputLayout.getEditText();
        RuntimeAssertion.require(mAddressEditText != null);

        mAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // clear old error if present when user is re-editing the text
                mAddressInputLayout.setErrorEnabled(false);
                mAddressInputLayout.setError(null);
            }
        });

        mRegisterButton = (Button) view.findViewById(R.id.button_register);
        RuntimeAssertion.require(mRegisterButton != null);

        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    String address = mAddressEditText.getText().toString() +
                            getResources().getText(R.string.kullo_domain);
                    startAddressCreation(address);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setRegisterAddressObserver();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterRegisterAddressObserver();
    }

    private boolean validateInput() {
        String text = mAddressEditText.getText().toString();

        if (text.isEmpty()) {
            mAddressInputLayout.setError(getResources().getText(R.string.choose_address_username_is_empty));
            mAddressInputLayout.setErrorEnabled(true);
            return false;
        }

        String address = text + getResources().getText(R.string.kullo_domain);
        if (!KulloUtils.isValidKulloAddress(address)) {
            mAddressInputLayout.setError(getResources().getText(R.string.choose_address_address_is_invalid));
            mAddressInputLayout.setErrorEnabled(true);
            return false;
        }

        return true;
    }

    public void startAddressCreation(final String addressString) {
        // prevent re-click
        mRegisterButton.setEnabled(false);

        SessionConnector.get().registerAddressAsync(addressString);
    }

    private void setRegisterAddressObserver() {
        mRegistrationRegisterAccountListenerObserver = new RegistrationRegisterAccountListenerObserver() {
            @Override
            public void challengeNeeded(String address, Challenge challenge) {
                Log.i(TAG, "Challenge of type " + challenge.type().toString() + " needed: '" + challenge.text() + "'");

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showRegistrationError(getResources().getString(R.string.choose_address_challenge_needed));
                    }
                });
            }

            @Override
            public void addressNotAvailable(String address, final AddressNotAvailableReason reason) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String errorText;
                        switch (reason) {
                            case BLOCKED:
                                errorText = getResources().getString(R.string.choose_address_address_blocked);
                                break;
                            case EXISTS:
                                errorText = getResources().getString(R.string.choose_address_address_already_exists);
                                break;
                            default:
                                throw new AssertionError("Invalid enum value.");
                        }
                        showRegistrationError(errorText);
                    }
                });
            }

            @Override
            public void finished(final Address address, final MasterKey masterKey) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SessionConnector.get().storeCredentials(getActivity(), new Credentials(address, masterKey));
                        openNextView();
                    }
                });
            }
            @Override
            public void error(Address address, final NetworkError error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRegisterButton.setEnabled(true);
                        DialogMaker.makeForNetworkError(getActivity(), error).show();
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(
                RegistrationRegisterAccountListenerObserver.class,
                mRegistrationRegisterAccountListenerObserver);
    }

    @UiThread
    private void showRegistrationError(final String errorText) {
        mFragmentRoot.requestFocus(); // Let user review input before changing the address

        Log.d(TAG, "Registration error: '" + errorText + "'");
        mAddressInputLayout.setErrorEnabled(true);
        mAddressInputLayout.setError(errorText);

        // re-enable button for more tries
        mRegisterButton.setEnabled(true);
    }

    private void unregisterRegisterAddressObserver() {
        if (mRegistrationRegisterAccountListenerObserver != null) {
            SessionConnector.get().removeListenerObserver(RegistrationRegisterAccountListenerObserver.class, mRegistrationRegisterAccountListenerObserver);
            mRegistrationRegisterAccountListenerObserver = null;
        }
    }

    @UiThread
    public void openNextView() {
        ((RegistrationActivity) getActivity()).nextFragment();
    }
}
