/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.screens.registration.ChooseAddressFragment;
import net.kullo.android.screens.registration.KeygenFragment;
import net.kullo.android.screens.registration.EnterAccountInfoFragment;

import java.util.ArrayList;

public class RegistrationActivity extends AppCompatActivity {
    private ArrayList<Fragment> mFragmentQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Ui.setColorStatusBarArrangeHeader(this);

        populateFragmentQueue();
        nextFragment();
    }

    @Override
    public void onBackPressed() {
        getToLoginActivity();
    }

    public void getToLoginActivity() {
        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        finish();
    }

    private void populateFragmentQueue() {
        mFragmentQueue = new ArrayList<Fragment>();
        mFragmentQueue.add(new KeygenFragment());
        mFragmentQueue.add(new ChooseAddressFragment());
        mFragmentQueue.add(new EnterAccountInfoFragment());
    }

    public void nextFragment() {
        if (mFragmentQueue.isEmpty()) {
            getToLoginActivity();
            return;
        }

        Fragment fragment = mFragmentQueue.remove(0);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.registration_fragment_holder, fragment);
        fragmentTransaction.commit();
    }
}
