/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.os.Bundle;

import net.kullo.android.R;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.littlehelpers.Ui;

public class ImpressumActivity extends KulloActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impressum);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);
    }
}
