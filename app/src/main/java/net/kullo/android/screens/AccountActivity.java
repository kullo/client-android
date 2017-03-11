/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.javautils.RuntimeAssertion;

public class AccountActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_account);

        Ui.prepareActivityForTaskManager(this);
        Ui.setStatusBarColor(this);
        Ui.setupActionbar(this);

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        GcmConnector.get().removeAllNotifications(this);

        // the fields are populated each time we restore because the user could have
        // logged in with a different account since the last time the activity had been created
        TextView addressView = (TextView) findViewById(R.id.account_address);
        TextView masterkeyView = (TextView) findViewById(R.id.account_masterkey);
        masterkeyView.setHorizontallyScrolling(true);

        addressView.setText(SessionConnector.get().getCurrentUserAddressAsString());
        masterkeyView.setText(SessionConnector.get().getCurrentUserMasterKeyAsPem());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
