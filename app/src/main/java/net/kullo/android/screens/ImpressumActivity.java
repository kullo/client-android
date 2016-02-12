/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.Ui;

public class ImpressumActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impressum);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);
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
