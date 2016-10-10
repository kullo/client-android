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
import net.kullo.android.notifications.GcmConnector;
import net.kullo.javautils.RuntimeAssertion;

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

        // app features
        String googlePlayStatusText;
        if (GcmConnector.get().googlePlayAvailable()) {
            googlePlayStatusText = getString(R.string.about_gplay_available);
        } else {
            GcmConnector.NotAvailableReason googlePlayNotAvailableReason = GcmConnector.get().googlePlayNotAvailableReason();
            RuntimeAssertion.require(googlePlayNotAvailableReason != null);

            String googlePlayNotAvailableReasonText = null;
            switch (googlePlayNotAvailableReason) {
                case Disabled:
                    googlePlayNotAvailableReasonText = getString(R.string.about_gplay_not_available_status_disabled);
                    break;
                case Missing:
                    googlePlayNotAvailableReasonText = getString(R.string.about_gplay_not_available_status_missing);
                    break;
                case UpdateRequired:
                    googlePlayNotAvailableReasonText = getString(R.string.about_gplay_not_available_status_update_required);
                    break;
                case Updating:
                    googlePlayNotAvailableReasonText = getString(R.string.about_gplay_not_available_status_updating);
                    break;
                case Unknown:
                    googlePlayNotAvailableReasonText = getString(R.string.about_gplay_not_available_status_unknown);
                    break;
                default:
                    RuntimeAssertion.fail("Unhandled enum value");
            }

            googlePlayStatusText = String.format(
                    getString(R.string.about_gplay_not_available),
                    googlePlayNotAvailableReasonText);
        }

        String pushText = getResources().getString(R.string.about_push_notifications_text);
        ((TextView) findViewById(R.id.about_push_notifications_text)).setText(String.format(pushText, googlePlayStatusText));
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
