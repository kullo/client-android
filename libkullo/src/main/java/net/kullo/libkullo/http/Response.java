// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

import java.util.ArrayList;

public final class Response {


    /*package*/ final ResponseError error;

    /*package*/ final int statusCode;

    /*package*/ final ArrayList<HttpHeader> headers;

    public Response(
            ResponseError error,
            int statusCode,
            ArrayList<HttpHeader> headers) {
        this.error = error;
        this.statusCode = statusCode;
        this.headers = headers;
    }

    public ResponseError getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ArrayList<HttpHeader> getHeaders() {
        return headers;
    }
}
