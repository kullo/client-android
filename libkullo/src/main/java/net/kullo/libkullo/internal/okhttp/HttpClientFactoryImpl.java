/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.internal.okhttp;

import net.kullo.libkullo.http.HttpClient;
import net.kullo.libkullo.http.HttpClientFactory;

import java.util.HashMap;

import okhttp3.OkHttpClient;

public class HttpClientFactoryImpl extends HttpClientFactory {
    private final OkHttpClient mClient = new OkHttpClient();

    @Override
    public HttpClient createHttpClient() {
        return new HttpClientImpl(mClient);
    }

    @Override
    public HashMap<String, String> versions() {
        return new HashMap<>();
    }
}
