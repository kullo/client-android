// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from session.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** Listener used in Session.accountInfoAsync() */
public abstract class SessionAccountInfoListener {
    public abstract void finished(@NonNull AccountInfo accountInfo);

    public abstract void error(@NonNull NetworkError error);
}
