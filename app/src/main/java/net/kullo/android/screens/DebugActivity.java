/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.Debug;

public class DebugActivity extends AppCompatActivity {
    private static final String TAG = "DebugActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        if (getIntent().getData() != null) {
            Uri intentUri = getIntent().getData();
            Log.d(TAG, Debug.getUriDetails(intentUri));
            return;
        }

        Uri testUri1 = Uri.parse("http://simon#kullo.net");
        Uri testUri2 = Uri.parse("http://example.com/#section2");
        Uri testUri3 = Uri.parse("kullo:simon#kullo.net");
        Uri testUri4 = Uri.parse("kullo:simon%23kullo.net");
        Log.d(TAG, Debug.getUriDetails(testUri1));
        Log.d(TAG, Debug.getUriDetails(testUri2));
        Log.d(TAG, Debug.getUriDetails(testUri3));
        Log.d(TAG, Debug.getUriDetails(testUri4));

        Intent i = new Intent(this, DebugActivity.class);
        i.setData(testUri2);
        Log.d(TAG, Debug.getIntentDetails(i));
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        float oneDpInPx = getResources().getDimension(R.dimen.debug_dp);
        float oneSpInPx = getResources().getDimension(R.dimen.debug_sp);
        float oneSpInDp = oneSpInPx / oneDpInPx;
        Log.d(TAG, "One dp in px: " + oneDpInPx);
        Log.d(TAG, "One sp in px: " + oneSpInPx);
        Log.d(TAG, "One sp in dp: " + oneSpInDp);
    }
}
