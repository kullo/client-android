/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.text.Editable;
import android.text.TextWatcher;

public class KulloConstants {
    public static final int REQUEST_CODE_NEW_MESSAGE = 42;

    // Intent keys
    public static final String MESSAGE_ID = "message_id";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String CONVERSATION_RECIPIENT = "conversation_recipient";

    // shared preferences
    public static final String ACCOUNT_PREFS_PLAIN = "net.kullo.android.SETTINGS";
    // obsolete preferences (keep to clear existing sensitive user data)
    public static final String ACCOUNT_PREFS_OBSOLETE = "net.kullo.android.ACCOUNT_PREFS";
    public static final String USER_SETTINGS_PREFS_OBSOLETE = "net.kullo.android.USER_SETTINGS_PREFS";

    public static final String KULLO_ADDRESS = "kullo_address";
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

    //image
    public static final int AVATAR_DIMENSION = 200;
    public static final int AVATAR_MAX_SIZE = 24*1024;
    public static final int AVATAR_BEST_QUALITY = 96;
    public static final int AVATAR_QUALITY_DOWNSAMPLING_STEPS = 2;
    public static final int AVATAR_MAX_ALLOWED_SIDE = 640;

    public static final int BLOCK_SIZE = 6;

    public static final String ACTION_SYNC = "net.kullo.action.SYNC";

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
