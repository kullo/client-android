// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from syncer.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    /**
     * Set or replace the SyncerListener which should receive sync events.
     *
     * Thread-safe.
     */
    public abstract void setListener(@NonNull SyncerListener listener);

    /**
     * Get the finishing time of the last successful full sync.
     * A full sync is one with "WithoutAttachments" or "Everything" sync mode.
     * Returns null if there hasn't been a sync yet.
     *
     * Thread-safe.
     */
    @Nullable
    public abstract DateTime lastFullSync();

    /**
     * Request that the data specified in mode is synced.
     *
     * Thread-safe.
     */
    public abstract void requestSync(@NonNull SyncMode mode);

    /**
     * Request that all attachments for the given message are downloaded.
     *
     * Thread-safe.
     */
    public abstract void requestDownloadingAttachmentsForMessage(long msgId);

    /**
     * Cancels the running sync and enqueued syncs, but doesn't wait for
     * termination. Stops all callbacks, even if the task continues to run.
     *
     * Thread-safe.
     */
    public abstract void cancel();

    /**
     * Returns true iff a sync is currently running.
     *
     * Thread-safe.
     */
    public abstract boolean isSyncing();

    /**
     * Blocks until the running sync and all enqueued syncs have finished.
     *
     * Thread-safe.
     */
    public abstract void waitUntilDone();

    /**
     * Blocks until the sync and all enqueued syncs have finished executing or
     * until the timeout has expired. Returns false on timeout, true otherwise.
     *
     * Thread-safe.
     */
    public abstract boolean waitForMs(int timeout);

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
        public DateTime lastFullSync()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_lastFullSync(this.nativeRef);
        }
        private native DateTime native_lastFullSync(long _nativeRef);

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
        public void cancel()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_cancel(this.nativeRef);
        }
        private native void native_cancel(long _nativeRef);

        @Override
        public boolean isSyncing()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_isSyncing(this.nativeRef);
        }
        private native boolean native_isSyncing(long _nativeRef);

        @Override
        public void waitUntilDone()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_waitUntilDone(this.nativeRef);
        }
        private native void native_waitUntilDone(long _nativeRef);

        @Override
        public boolean waitForMs(int timeout)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_waitForMs(this.nativeRef, timeout);
        }
        private native boolean native_waitForMs(long _nativeRef, int timeout);
    }
}
