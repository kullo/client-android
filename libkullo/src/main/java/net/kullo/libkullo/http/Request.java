// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

import java.util.ArrayList;

public final class Request {


    /*package*/ final HttpMethod method;

    /*package*/ final String url;

    /*package*/ final ArrayList<HttpHeader> headers;

    public Request(
            HttpMethod method,
            String url,
            ArrayList<HttpHeader> headers) {
        this.method = method;
        this.url = url;
        this.headers = headers;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public ArrayList<HttpHeader> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "Request{" +
                "method=" + method +
                "," + "url=" + url +
                "," + "headers=" + headers +
        "}";
    }

}
