/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.libkullo.api.AsyncTask;

public class AccountActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_account);

        Ui.setColorStatusBarArrangeHeader(this);
        Ui.setupActionbar(this);

        if (task != null) task.waitUntilDone();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // the fields are populated each time we restore because the user could have
        // logged in with a different account since the last time the activity had been created
        TextView addressView = (TextView) findViewById(R.id.account_address);
        TextView masterkeyView = (TextView) findViewById(R.id.account_masterkey);
        masterkeyView.setHorizontallyScrolling(true);

        addressView.setText(KulloConnector.get().getClientAddressAsString());
        masterkeyView.setText(KulloConnector.get().getMasterKeyAsPem());
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
