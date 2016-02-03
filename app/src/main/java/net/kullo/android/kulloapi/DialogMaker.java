/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.content.Context;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;

public class DialogMaker {
    @NonNull
    public static MaterialDialog makeForNetworkError(@NonNull Context context, NetworkError error) {
        String title = context.getString(R.string.network_error_title);
        String description = getTextForNetworkError(context, error);

        return new MaterialDialog.Builder(context)
                .title(title)
                .content(description)
                .neutralText(R.string.ok)
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                }).build();
    }

    @NonNull
    public static MaterialDialog makeForLocalError(@NonNull Context context, LocalError error) {
        return new MaterialDialog.Builder(context)
                .title("Local error")
                .build();
    }

    @NonNull
    public static String getTextForNetworkError(@NonNull Context context, NetworkError error) {
        switch (error) {
            case FORBIDDEN:
                return context.getString(R.string.network_error_forbidden);
            case PROTOCOL:
                return context.getString(R.string.network_error_protocol);
            case UNAUTHORIZED:
                return context.getString(R.string.network_error_unauthorized);
            case SERVER:
                return context.getString(R.string.network_error_server);
            case CONNECTION:
                return context.getString(R.string.network_error_connection);
            case UNKNOWN:
                return context.getString(R.string.network_error_unknown);
            default:
                throw new AssertionError("Invalid value of NetworkError");
        }
    }
}
