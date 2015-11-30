/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.application;

import android.content.Context;
import android.content.Intent;

import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.screens.LoginActivity;

public class LogoutIntent extends Intent {
    public LogoutIntent(Context context) {
        super(context, LoginActivity.class);

        putExtra(KulloConstants.LOGOUT_INTENT, true);
    }
}
