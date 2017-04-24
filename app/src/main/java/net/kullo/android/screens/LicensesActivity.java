/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.os.Bundle;
import android.webkit.WebView;

import net.kullo.android.R;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.javautils.RuntimeAssertion;

public class LicensesActivity extends KulloActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);

        WebView webview = (WebView) findViewById(R.id.webview);
        RuntimeAssertion.require(webview != null);
        webview.loadUrl(KulloApplication.LICENSES_FILE);
    }
}
