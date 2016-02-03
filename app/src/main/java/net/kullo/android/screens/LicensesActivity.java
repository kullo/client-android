/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.javautils.RuntimeAssertion;

public class LicensesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        WebView webview = (WebView) findViewById(R.id.webview);
        RuntimeAssertion.require(webview != null);
        webview.loadUrl(KulloApplication.LICENSES_FILE);
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
