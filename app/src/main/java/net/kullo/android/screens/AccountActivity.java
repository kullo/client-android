/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.libkullo.api.AsyncTask;

public class AccountActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = SessionConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_account);

        Ui.setColorStatusBarArrangeHeader(this);
        Ui.setupActionbar(this);

        if (task != null) task.waitUntilDone();

        GcmConnector.get().fetchToken(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // the fields are populated each time we restore because the user could have
        // logged in with a different account since the last time the activity had been created
        TextView addressView = (TextView) findViewById(R.id.account_address);
        TextView masterkeyView = (TextView) findViewById(R.id.account_masterkey);
        masterkeyView.setHorizontallyScrolling(true);

        addressView.setText(SessionConnector.get().getClientAddressAsString());
        masterkeyView.setText(SessionConnector.get().getMasterKeyAsPem());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
