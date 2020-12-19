/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
