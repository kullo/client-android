// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from log.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** Listener used in Registry.setLogListener() */
public abstract class LogListener {
    /** Called whenever libkullo produces a log message. */
    public abstract void writeLogMessage(@NonNull String file, int line, @NonNull String function, @NonNull LogType type, @NonNull String message, @NonNull String thread, @NonNull String stacktrace);
}
