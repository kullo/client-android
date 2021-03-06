/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.Arrays;
import java.util.List;

public class KulloConstants {
    public static final int REQUEST_CODE_NEW_MESSAGE = 42;
    public static final int REQUEST_CODE_SEND_MESSAGE_WITH_SHARE = 43;

    // Intent keys
    public static final String MESSAGE_ID = "message_id";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String CONVERSATION_RECIPIENT = "conversation_recipient";
    public static final String CONVERSATION_ADD_ATTACHMENTS = "conversation_add_attachments";
    public static final String CONVERSATION_ADD_TEXT = "conversation_add_text";

    // shared preferences files
    public static final String PREFS_FILE_DEFAULT = "net.kullo.android.SETTINGS";
    // obsolete preferences files (keep to clear existing sensitive user data)
    public static final String PREFS_FILE_OBSOLETE_ACCOUNT = "net.kullo.android.ACCOUNT_PREFS";
    public static final String PREFS_FILE_OBSOLETE_USER_SETTINGS = "net.kullo.android.USER_SETTINGS_PREFS";

    public static final String SEPARATOR = "|";
    public static final String KULLO_ADDRESS = "kullo_address";
    public static final String PREFS_KEY_VERSION = "net.kullo.android.SETTINGS_VERSION";
    public static final String ACTIVE_USER = "active_user";
    public static final String LAST_ACTIVE_USER = "last_active_user";
    public static final String BLOCK_A = "block_a";
    public static final String BLOCK_B = "block_b";
    public static final String BLOCK_C = "block_c";
    public static final String BLOCK_D = "block_d";
    public static final String BLOCK_E = "block_e";
    public static final String BLOCK_F = "block_f";
    public static final String BLOCK_G = "block_g";
    public static final String BLOCK_H = "block_h";
    public static final String BLOCK_I = "block_i";
    public static final String BLOCK_J = "block_j";
    public static final String BLOCK_K = "block_k";
    public static final String BLOCK_L = "block_l";
    public static final String BLOCK_M = "block_m";
    public static final String BLOCK_N = "block_n";
    public static final String BLOCK_O = "block_o";
    public static final String BLOCK_P = "block_p";
    public static final List<String> BLOCK_KEYS_AS_LIST = Arrays.asList(
            KulloConstants.BLOCK_A,
            KulloConstants.BLOCK_B,
            KulloConstants.BLOCK_C,
            KulloConstants.BLOCK_D,
            KulloConstants.BLOCK_E,
            KulloConstants.BLOCK_F,
            KulloConstants.BLOCK_G,
            KulloConstants.BLOCK_H,
            KulloConstants.BLOCK_I,
            KulloConstants.BLOCK_J,
            KulloConstants.BLOCK_K,
            KulloConstants.BLOCK_L,
            KulloConstants.BLOCK_M,
            KulloConstants.BLOCK_N,
            KulloConstants.BLOCK_O,
            KulloConstants.BLOCK_P
    );

    //image
    public static final int AVATAR_DIMENSION = 200;
    public static final int AVATAR_MAX_SIZE = 24*1024;
    public static final int AVATAR_BEST_QUALITY = 96;
    public static final int AVATAR_QUALITY_DOWNSAMPLING_STEPS = 2;

    // 8 mio pixel limit => 5392x1236 pixel for a 10 MB 10784x2472 panorama
    // picture after downsampling with size 2 (i.e. 2x2 pixels -> 1 pixel),
    // which is 27 MB in memory.
    public static final int AVATAR_PREVIEW_MAX_PIXEL_COUNT = 8_000_000;

    public static final int BLOCK_SIZE = 6;

    public static final String ACTION_SYNC = "net.kullo.action.SYNC";

    // Share receiver
    public static final int SHARE_TEXT_LIMIT = 10_000; // unicode chars

    public static final TextWatcher KULLO_ADDRESS_AT_THIEF = new TextWatcher() {
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void afterTextChanged(Editable s) {
            int len = s.length();
            if (len > 0) {
                if (s.charAt(len-1) == '@') {
                    s.replace(len-1, len, "#");
                }
            }
        }
    };
}
