/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi.attachments;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.bytes.BytesResource;

import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.SessionConnector.GetMessageAttachmentCallback;
import net.kullo.android.littlehelpers.ObjectContainer;
import net.kullo.libkullo.api.AsyncTask;

public class AttachmentFromDatabaseLoader implements ModelLoader<AttachmentIdentifier, BytesResource> {

    private static final String TAG = AttachmentFromDatabaseLoader.class.getSimpleName();

    @Override
    public DataFetcher<BytesResource> getResourceFetcher(final AttachmentIdentifier model, final int width, final int height) {
        return new DataFetcher<BytesResource>() {
            @Override
            public BytesResource loadData(Priority priority) throws Exception {
                Log.d(TAG, "Loading " + model.toString() + " from database");

                final ObjectContainer<BytesResource> outDataWrapper = new ObjectContainer<>();
                final ObjectContainer<AsyncTask> taskWrapper = new ObjectContainer<>();

                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        taskWrapper.set(SessionConnector.get().getMessageAttachment(
                            model.messageId, model.attachmentId, new GetMessageAttachmentCallback() {
                            @Override
                            public void run(byte[] data) {
                                outDataWrapper.set(new BytesResource(data));
                            }
                        }));

                        synchronized(this) {
                            this.notify();
                        }
                    }
                };

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized(r) {
                    new Handler(Looper.getMainLooper()).post(r);

                    // r is unlocked while waiting
                    // http://stackoverflow.com/a/5996961/2013738
                    r.wait();
                }

                taskWrapper.get().waitUntilDone();

                return outDataWrapper.get();
            }

            @Override
            public void cleanup() {

            }

            @Override
            public String getId() {
                final Uri thisDataFetcherUri = model.asUri().buildUpon()
                    .appendQueryParameter("width", String.valueOf(width))
                    .appendQueryParameter("height", String.valueOf(height))
                    .build();
                Log.d(TAG, thisDataFetcherUri.toString());
                return thisDataFetcherUri.toString();
            }

            @Override
            public void cancel() {

            }
        };
    }
}
