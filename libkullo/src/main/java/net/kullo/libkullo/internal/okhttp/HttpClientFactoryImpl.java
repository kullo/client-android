/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.internal.okhttp;

import net.kullo.libkullo.http.HttpClient;
import net.kullo.libkullo.http.HttpClientFactory;

import java.util.HashMap;

public class HttpClientFactoryImpl extends HttpClientFactory {
    @Override
    public HttpClient createHttpClient() {
        return new HttpClientImpl();
    }

    @Override
    public HashMap<String, String> versions() {
        return new HashMap<>();
    }
}
