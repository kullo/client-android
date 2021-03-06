// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.HashMap;

public abstract class HttpClientFactory {
    @NonNull
    public abstract HttpClient createHttpClient();

    /** Returns pairs of <library name, version number> for the libraries used. */
    @NonNull
    public abstract HashMap<String, String> versions();
}
