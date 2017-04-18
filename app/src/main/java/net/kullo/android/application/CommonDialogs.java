/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.application;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import net.kullo.android.R;
import net.kullo.android.screens.ProfileSettingsActivity;

import io.github.dialogsforandroid.DialogAction;
import io.github.dialogsforandroid.MaterialDialog;

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
                            callingActivity.startActivity(new Intent(callingActivity, ProfileSettingsActivity.class));
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
