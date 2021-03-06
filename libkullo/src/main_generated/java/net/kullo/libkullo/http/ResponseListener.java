// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ResponseListener {
    /**
     * Called when there's progress made on upload or download.
     * The request can be canceled by returning the appropriate code.
     */
    @NonNull
    public abstract ProgressResult progressed(@NonNull TransferProgress progress);

    public abstract void dataReceived(@NonNull byte[] data);

    private static final class CppProxy extends ResponseListener
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
        public ProgressResult progressed(TransferProgress progress)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_progressed(this.nativeRef, progress);
        }
        private native ProgressResult native_progressed(long _nativeRef, TransferProgress progress);

        @Override
        public void dataReceived(byte[] data)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_dataReceived(this.nativeRef, data);
        }
        private native void native_dataReceived(long _nativeRef, byte[] data);
    }
}
