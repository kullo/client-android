// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles syncing, including downloading of attachments. Prevents multiple syncs
 * from running in parallel by building a queue and intelligently merging sync
 * requests.
 * Examples: Enqueuing a WithoutAttachments sync removes a SendOnly sync from the
 * queue. A running SendOnly sync would be cancelled. Enqueuing a SendOnly sync
 * while a WithoutAttachments sync is running or enqueued will do nothing.
 * Enqueuing an Everything sync will remove all attachment download requests from
 * the queue.
 */
public abstract class Syncer {
    /** Set or replace the SyncerListener which should receive sync events. */
    public abstract void setListener(SyncerListener listener);

    /** Request that the data specified in mode is synced. */
    public abstract void requestSync(SyncMode mode);

    /** Request that all attachments for the given message are downloaded. */
    public abstract void requestDownloadingAttachmentsForMessage(long msgId);

    /**
     * Gets an AsyncTask that can be used for cancellation or waiting. It affects
     * both running and queued jobs. Releasing it will not cancel the job
     * automatically.
     */
    public abstract AsyncTask asyncTask();

    private static final class CppProxy extends Syncer
    {
        private final long nativeRef;
        private final AtomicBoolean destroyed = new AtomicBoolean(false);

        private CppProxy(long nativeRef)
        {
            if (nativeRef == 0) throw new RuntimeException("nativeRef is zero");
            this.nativeRef = nativeRef;
        }

        private native void nativeDestroy(long nativeRef);
        public void destroy()
        {
            boolean destroyed = this.destroyed.getAndSet(true);
            if (!destroyed) nativeDestroy(this.nativeRef);
        }
        protected void finalize() throws java.lang.Throwable
        {
            destroy();
            super.finalize();
        }

        @Override
        public void setListener(SyncerListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setListener(this.nativeRef, listener);
        }
        private native void native_setListener(long _nativeRef, SyncerListener listener);

        @Override
        public void requestSync(SyncMode mode)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_requestSync(this.nativeRef, mode);
        }
        private native void native_requestSync(long _nativeRef, SyncMode mode);

        @Override
        public void requestDownloadingAttachmentsForMessage(long msgId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_requestDownloadingAttachmentsForMessage(this.nativeRef, msgId);
        }
        private native void native_requestDownloadingAttachmentsForMessage(long _nativeRef, long msgId);

        @Override
        public AsyncTask asyncTask()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_asyncTask(this.nativeRef);
        }
        private native AsyncTask native_asyncTask(long _nativeRef);
    }
}
