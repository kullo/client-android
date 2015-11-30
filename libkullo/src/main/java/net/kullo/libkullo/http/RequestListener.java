// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from http.djinni

package net.kullo.libkullo.http;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RequestListener {
    public abstract byte[] read(long maxSize);

    private static final class CppProxy extends RequestListener
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
        public byte[] read(long maxSize)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_read(this.nativeRef, maxSize);
        }
        private native byte[] native_read(long _nativeRef, long maxSize);
    }
}
