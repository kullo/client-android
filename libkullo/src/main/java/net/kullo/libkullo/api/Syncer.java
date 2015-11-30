// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Syncer {
    /** Synchronize messages etc. with the server. */
    public abstract AsyncTask runAsync(SyncMode mode, SyncerRunListener listener);

    /** Download all attachments for a given message */
    public abstract AsyncTask downloadAttachmentsForMessageAsync(long msgId, SyncerRunListener listener);

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
        public AsyncTask runAsync(SyncMode mode, SyncerRunListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_runAsync(this.nativeRef, mode, listener);
        }
        private native AsyncTask native_runAsync(long _nativeRef, SyncMode mode, SyncerRunListener listener);

        @Override
        public AsyncTask downloadAttachmentsForMessageAsync(long msgId, SyncerRunListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_downloadAttachmentsForMessageAsync(this.nativeRef, msgId, listener);
        }
        private native AsyncTask native_downloadAttachmentsForMessageAsync(long _nativeRef, long msgId, SyncerRunListener listener);
    }
}
