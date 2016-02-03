/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.internal.okhttp;

import android.util.Log;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;

import net.kullo.libkullo.http.HttpClient;
import net.kullo.libkullo.http.HttpHeader;
import net.kullo.libkullo.http.HttpMethod;
import net.kullo.libkullo.http.ProgressResult;
import net.kullo.libkullo.http.Request;
import net.kullo.libkullo.http.RequestListener;
import net.kullo.libkullo.http.Response;
import net.kullo.libkullo.http.ResponseError;
import net.kullo.libkullo.http.ResponseListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;

public class HttpClientImpl extends HttpClient {
    private static final int BUFFER_SIZE = 64 * 1024;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public Response sendRequest(Request request, long timeout, final RequestListener requestListener, ResponseListener responseListener) {
        com.squareup.okhttp.Request.Builder requestBuilder = new com.squareup.okhttp.Request.Builder();

        // make a (shallow) clone of the client instance to customize client settings per request
        OkHttpClient configuredClient = client.clone();

        // set timeouts
        client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
        client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);

        // prepare progress reporting
        final Progress progress = new Progress(responseListener);

        // set URL
        requestBuilder.url(request.getUrl());

        // set request headers
        String contentType = "text/plain";
        for (HttpHeader header : request.getHeaders()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());

            if (header.getKey().equalsIgnoreCase("content-type"))
            {
                contentType = header.getValue();
            }
        }

        // set method and configure request body
        RequestBody requestBody = null;
        switch (request.getMethod()) {
            case POST:
            case PUT:
            case PATCH:
                // we need a copy because it needs to be final to be used from inner class
                final String contentTypeHeader = contentType;

                requestBody = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse(contentTypeHeader);
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        byte[] outBuf;
                        while ((outBuf = requestListener.read(BUFFER_SIZE)).length > 0) {
                            sink.write(outBuf);
                            progress.addToTx(outBuf.length);
                        }
                    }
                };
        }
        requestBuilder.method(getRequestMethodString(request.getMethod()), requestBody);

        // build request
        com.squareup.okhttp.Request httpRequest = requestBuilder.build();

        // execute request
        ArrayList<HttpHeader> responseHeaders = new ArrayList<>();
        com.squareup.okhttp.Response response;
        InputStream responseBody = null;
        try {
            progress.throwIfCanceled();
            response = configuredClient.newCall(httpRequest).execute();

            // read response headers
            Headers headers = response.headers();
            for (int i = 0; i < headers.size(); ++i) {
                responseHeaders.add(new HttpHeader(headers.name(i), headers.value(i)));
            }

            // read response body
            responseBody = response.body().byteStream();
            byte[] inBuf = new byte[BUFFER_SIZE];
            int bytesReceived;
            while ((bytesReceived = responseBody.read(inBuf)) > -1) {
                byte[] data = Arrays.copyOf(inBuf, bytesReceived);
                responseListener.dataReceived(data);
                progress.addToRx(bytesReceived);
            }

        } catch (RequestCanceled e) {
            return new Response(ResponseError.CANCELED, 0, responseHeaders);

        } catch (SocketTimeoutException e) {
            return new Response(ResponseError.TIMEOUT, 0, responseHeaders);

        } catch (IOException e) {
            return new Response(ResponseError.NETWORKERROR, 0, responseHeaders);

        } finally {
            try {
                if (responseBody != null) responseBody.close();
            } catch (IOException e) {
                // If we can't close it, let it open... We can't do anything else.
            }
        }

        return new Response(null, response.code(), responseHeaders);
    }

    private String getRequestMethodString(HttpMethod method) {
        String result = null;
        switch (method) {
            case GET:
                result = "GET";
                break;
            case POST:
                result = "POST";
                break;
            case PUT:
                result = "PUT";
                break;
            case PATCH:
                result = "PATCH";
                break;
            case DELETE:
                result = "DELETE";
                break;
            default:
                Log.e("HttpClientImpl", "unknown request method: " + method);
        }
        return result;
    }

    private void logException(String message, Throwable throwable) {
        Log.e("HttpClientImpl",
                message + ": " + throwable.getClass().getName() + ": " + throwable.getMessage(),
                throwable);
    }

    private class RequestCanceled extends IOException {
    }

    private class Progress {
        public Progress(ResponseListener responseListener) {
            this.responseListener = responseListener;
        }

        public void addToTx(int deltaTx) throws RequestCanceled {
            txCurrent += deltaTx;
            txTotal = Math.max(txTotal, txCurrent);
            updateAndCheckCanceled();
        }

        public void addToRx(int deltaRx) throws RequestCanceled {
            rxCurrent += deltaRx;
            rxTotal = Math.max(rxTotal, rxCurrent);
            updateAndCheckCanceled();
        }

        public void throwIfCanceled() throws RequestCanceled {
            updateAndCheckCanceled();
        }

        private void updateAndCheckCanceled() throws RequestCanceled {
            if (responseListener.progress(txCurrent, txTotal, rxCurrent, rxTotal) == ProgressResult.CANCEL) {
                throw new RequestCanceled();
            }
        }

        private ResponseListener responseListener;
        private long txCurrent = 0, txTotal = 0, rxCurrent = 0, rxTotal = 0;
    }
}
