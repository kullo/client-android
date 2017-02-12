/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.internal.okhttp;

import android.util.Log;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import net.kullo.libkullo.http.HttpClient;
import net.kullo.libkullo.http.HttpHeader;
import net.kullo.libkullo.http.HttpMethod;
import net.kullo.libkullo.http.ProgressResult;
import net.kullo.libkullo.http.Request;
import net.kullo.libkullo.http.RequestListener;
import net.kullo.libkullo.http.Response;
import net.kullo.libkullo.http.ResponseError;
import net.kullo.libkullo.http.ResponseListener;
import net.kullo.libkullo.http.TransferProgress;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;

public class HttpClientImpl extends HttpClient {
    private static final String TAG = "HttpClientImpl";
    private static final int BUFFER_SIZE = 64 * 1024; // bytes

    private final OkHttpClient mClient;

    public HttpClientImpl(OkHttpClient client) {
        super();
        mClient = client;
    }

    @Override
    public Response sendRequest(Request request, int timeoutMs, final RequestListener requestListener, ResponseListener responseListener) {
        if (timeoutMs < 0) throw new IllegalArgumentException("timeout must be >= 0");

        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder();

        // customize client settings per request
        final OkHttpClient.Builder clientBuilder = mClient.newBuilder();

        // set timeouts
        clientBuilder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        OkHttpClient client = clientBuilder.build();

        // prepare progress reporting
        final Progress progress = new Progress(responseListener);

        // set URL
        requestBuilder.url(request.getUrl());

        // set request headers
        String contentType = "text/plain";
        for (HttpHeader header : request.getHeaders()) {
            requestBuilder.header(header.getKey(), header.getValue());

            if (header.getKey().equalsIgnoreCase("content-type")) {
                contentType = header.getValue();
            } else if (header.getKey().equalsIgnoreCase("content-length")) {
                try {
                    progress.setTxTotal(Long.valueOf(header.getValue()));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Couldn't parse Content-Length header: " + header.getValue());
                }
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
        okhttp3.Request httpRequest = requestBuilder.build();

        // execute request
        ArrayList<HttpHeader> responseHeaders = new ArrayList<>();
        okhttp3.Response response;
        InputStream responseBody = null;
        try {
            progress.update();
            response = client.newCall(httpRequest).execute();

            // read response headers
            Headers headers = response.headers();
            for (int i = 0; i < headers.size(); ++i) {
                responseHeaders.add(new HttpHeader(headers.name(i), headers.value(i)));
            }

            // read response body
            progress.setRxTotal(response.body().contentLength());
            responseBody = response.body().byteStream();
            byte[] inBuf = new byte[BUFFER_SIZE];
            int bytesReceived;
            while ((bytesReceived = responseBody.read(inBuf)) > -1) {
                byte[] data = Arrays.copyOf(inBuf, bytesReceived);
                responseListener.dataReceived(data);
                progress.addToRx(bytesReceived);
            }
        } catch (RequestCanceled e) {
            Log.d(TAG, "RequestCanceled details: " + e.getMessage());
            return new Response(ResponseError.CANCELED, 0, responseHeaders);
        } catch (SocketTimeoutException e) {
            Log.d(TAG, "SocketTimeoutException details: " + e.getMessage());
            return new Response(ResponseError.TIMEOUT, 0, responseHeaders);
        } catch (IOException e) {
            Log.d(TAG, "IOException details: " + e.getMessage());
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
                Log.e(TAG, "unknown request method: " + method);
        }
        return result;
    }

    private void logException(String message, Throwable throwable) {
        Log.e(TAG, message + ": " + throwable.getClass().getName() + ": " + throwable.getMessage(),
                throwable);
    }

    private class RequestCanceled extends IOException {
    }

    private class Progress {
        public Progress(ResponseListener responseListener) {
            this.responseListener = responseListener;
        }

        public void setTxTotal(long value) {
            txTotal = Math.max(value, 0);
        }

        public void setRxTotal(long value) {
            rxTotal = Math.max(value, 0);
        }

        public void addToTx(int deltaTx) throws RequestCanceled {
            txCurrent += deltaTx;
            updateAndCheckCanceled();
        }

        public void addToRx(int deltaRx) throws RequestCanceled {
            rxCurrent += deltaRx;
            updateAndCheckCanceled();
        }

        public void update() throws RequestCanceled {
            updateAndCheckCanceled();
        }

        private void updateAndCheckCanceled() throws RequestCanceled {
            if (responseListener.progressed(
                    new TransferProgress(txCurrent, txTotal, rxCurrent, rxTotal))
                    == ProgressResult.CANCEL) {
                throw new RequestCanceled();
            }
        }

        private final ResponseListener responseListener;
        private long txCurrent = 0, txTotal = 0, rxCurrent = 0, rxTotal = 0;
    }
}
