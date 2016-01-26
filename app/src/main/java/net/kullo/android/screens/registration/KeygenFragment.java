/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.registration;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Timer;
import android.widget.TextView;
import java.util.TimerTask;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.observers.listenerobservers.ClientGenerateKeysListenerObserver;
import net.kullo.android.screens.RegistrationActivity;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class KeygenFragment extends Fragment {
    private MaterialProgressBar mProgressBar;
    private Timer mDotAnimation;
    private ClientGenerateKeysListenerObserver mGenerateKeysListenerObserver;
    private volatile byte mCurrentProgress = -1;
    private volatile boolean mIsFragmentStarted = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration_keygen, container, false);

        mProgressBar = (MaterialProgressBar) view.findViewById(R.id.masterkey_creation_progress);

        mDotAnimation = launchDotAnimation(
            (TextView) view.findViewById(R.id.masterkey_creation_title),
            getResources().getString(R.string.generate_keys_title)
            );

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mIsFragmentStarted = true;
        if (mCurrentProgress == -1) {
            // first start
            launchKeygenTask();
        }
        if (mCurrentProgress >= 100) {
            // keygen finished in the background
            openNextView();
        }
        mProgressBar.setProgress(mCurrentProgress);
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsFragmentStarted = false;
    }

    @Override
    public void onDestroy() {
        mDotAnimation.cancel();
        super.onDestroy();
    }

    private Timer launchDotAnimation(final TextView textView, final String baseStringPattern) {
        Timer anim = new Timer();
        anim.schedule(new TimerTask() {
                private int dotCount = 0;
                private final String dotString = "...";
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(String.format(baseStringPattern, dotString.substring(0, dotCount)));
                            dotCount = ++dotCount % 4;
                        }
                    });
                }
            },
            0,800); // launch now, repeat every 800 ms
        return anim;
    }

    private void launchKeygenTask() {
        mCurrentProgress = 0;
        registerGenerateKeysObserver();
        SessionConnector.get().generateKeysAsync();
    }

    private void registerGenerateKeysObserver() {
        mGenerateKeysListenerObserver = new ClientGenerateKeysListenerObserver() {
            @Override
            public void progress(final byte progress) {
                mCurrentProgress = progress;
                if (mIsFragmentStarted) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(progress);
                        }
                    });
                }
            }

            @Override
            public void finished() {
                mCurrentProgress = 100;
                if (mIsFragmentStarted) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            openNextView();
                        }
                    });
                }
            }
        };

        SessionConnector.get().addListenerObserver(
                ClientGenerateKeysListenerObserver.class,
                mGenerateKeysListenerObserver);
    }

    private void unregisterGenerateKeysObserver() {
        SessionConnector.get().removeListenerObserver(
                ClientGenerateKeysListenerObserver.class,
                mGenerateKeysListenerObserver);
    }

    @UiThread
    public void openNextView() {
        unregisterGenerateKeysObserver();
        mCurrentProgress = 0;
        mDotAnimation.cancel();
        ((RegistrationActivity) getActivity()).nextFragment();
    }
}
