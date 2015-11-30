/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.registration;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.observers.listenerobservers.ClientGenerateKeysListenerObserver;
import net.kullo.android.screens.RegistrationActivity;

public class KeygenFragment extends Fragment {
    private ProgressBar mProgressBar;
    private ClientGenerateKeysListenerObserver mGenerateKeysListenerObserver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration_keygen, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.masterkey_creation_progress);
        mProgressBar.setProgress(0);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        registerGenerateKeysObserver();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterGenerateKeysObserver();
    }

    @Override
    public void onResume() {
        super.onResume();

        KulloConnector.get().generateKeysAsync();
    }

    private void registerGenerateKeysObserver() {
        mGenerateKeysListenerObserver = new ClientGenerateKeysListenerObserver() {
            @Override
            public void progress(final byte progress) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setProgress(progress);
                    }
                });
            }

            @Override
            public void finished() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        openNextView();
                    }
                });
            }
        };

        KulloConnector.get().addListenerObserver(
                ClientGenerateKeysListenerObserver.class,
                mGenerateKeysListenerObserver);
    }

    private void unregisterGenerateKeysObserver() {
        KulloConnector.get().removeListenerObserver(
                ClientGenerateKeysListenerObserver.class,
                mGenerateKeysListenerObserver);
    }

    @UiThread
    public void openNextView() {
        ((RegistrationActivity) getActivity()).nextFragment();
    }
}
