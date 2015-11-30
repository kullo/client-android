// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

public abstract class HttpClient {
    /**
     * Synchronously send the given request.
     * Not thread-safe! Use a separate HttpClient instance per thread.
     *
     * * timeout is measured in milliseconds
     * * requestListener must be non-null if method is PATCH, POST or PUT
     * * responseListener may be null
     */
    public abstract Response sendRequest(Request request, long timeout, RequestListener requestListener, ResponseListener responseListener);
}
