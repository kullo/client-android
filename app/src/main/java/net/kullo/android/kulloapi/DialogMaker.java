/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.screens.ComposeActivity;
import net.kullo.android.screens.StartConversationActivity;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;

import java.util.LinkedList;
import java.util.List;

import io.github.dialogsforandroid.DialogAction;
import io.github.dialogsforandroid.MaterialDialog;

public class DialogMaker {
    @NonNull
    public static MaterialDialog makeForKulloAddress(@NonNull final Context context, @NonNull final Address address, final boolean isMe) {
        List<String> items = new LinkedList<>();
        if (!isMe) {
            items.add(context.getString(R.string.address_dialog_option_write));
            items.add(context.getString(R.string.address_dialog_option_add));
        }
        items.add(context.getString(R.string.address_dialog_option_copy));

        return new MaterialDialog.Builder(context)
            .title(address.toString())
            .items(items)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
                    if (isMe) {
                        which += 2;
                    }

                    switch (which) {
                        case 0: {
                            Intent intent = new Intent(context, ComposeActivity.class);
                            intent.putExtra(KulloConstants.CONVERSATION_RECIPIENT, address.toString());
                            context.startActivity(intent);
                            break;
                        }
                        case 1: {
                            KulloApplication.sharedInstance.startConversationParticipants.add(address);
                            context.startActivity(new Intent(context, StartConversationActivity.class));
                            break;
                        }
                        case 2: {
                            String label = context.getResources().getString(R.string.kullo_address);
                            String text = address.toString();
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(label, text);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context,
                                String.format(context.getString(R.string.address_dialog_confirmation_copied), text),
                                Toast.LENGTH_SHORT).show();
                            break;
                        }
                        default:
                            RuntimeAssertion.fail();
                    }
                }
            })
            .build();
    }

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
