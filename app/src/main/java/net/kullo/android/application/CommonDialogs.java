/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.application;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.screens.SettingsActivity;

public class CommonDialogs {
    public static MaterialDialog buildShowSettingsDialog(final Activity callingActivity) {
        return new MaterialDialog.Builder(callingActivity)
                    .title(R.string.new_message_settings_incomplete_dialog_title)
                    .content(R.string.new_message_settings_incomplete_dialog_content)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction dialogAction) {
                            callingActivity.startActivity(new Intent(callingActivity, SettingsActivity.class));
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction dialogAction) {
                            dialog.dismiss();
                        }
                    }).build();
    }
}
