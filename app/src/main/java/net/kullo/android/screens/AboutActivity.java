/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.AppVersion;
import net.kullo.android.littlehelpers.Ui;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        // Set app version
        String versionName = new AppVersion(getApplication()).versionName();
        String appVersionPattern = getResources().getString(R.string.app_version);
        ((TextView) findViewById(R.id.app_version)).setText(
                String.format(appVersionPattern, versionName));

        String kudosText = getResources().getString(R.string.licenses_text_kudos);
        String versionsText = ((KulloApplication) getApplication()).softwareVersions();
        ((TextView) findViewById(R.id.kudos_text)).setText(String.format(kudosText, versionsText));
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

    public void openImpressumClicked(View view) {
        startActivity(new Intent(this, ImpressumActivity.class));
    }

    public void openWebsiteClicked(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, KulloApplication.MAINTAINER_WEBSITE));
    }

    public void openLicensesClicked(View view) {
        startActivity(new Intent(this, LicensesActivity.class));
    }
}
