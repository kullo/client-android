/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.internal;

import android.util.Log;

import net.kullo.libkullo.api.LogListener;
import net.kullo.libkullo.api.LogType;

public class AndroidLogListener extends LogListener {

    protected static final String tag = "libkullo";

    @Override
    public void writeLogMessage(String file, int line, String function, LogType type, String message, String thread, String stacktrace) {
        int priority;
        switch (type) {
            case DEBUG:
                priority = Log.DEBUG;
                break;
            case INFO:
                priority = Log.INFO;
                break;
            case WARNING:
                priority = Log.WARN;
                break;
            case ERROR:
                priority = Log.ERROR;
                break;
            case FATAL:
                priority = Log.ERROR;
                break;
            case NONE:
                Log.w(tag, "The following message has LogType = NONE, which should never happen.");
                priority = Log.ERROR;
                break;
            default:
                Log.w(tag, "The following message has an unknown LogType.");
                priority = Log.ERROR;
        }

        String logMsg = thread + " " + shortenSourcePath(file) + ":" + line + ": " + message;
        if (!stacktrace.isEmpty()) {
            logMsg += "\nStack:\n" + stacktrace;
        }

        Log.println(priority, tag, logMsg);
    }

    private String shortenSourcePath(String file) {
        // +1 makes sure that we start at 0 when not found, and after the slash otherwise
        return file.substring(file.lastIndexOf('/') + 1);
    }
}
