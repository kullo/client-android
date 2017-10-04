// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from registry.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

/** Registry for helpers that are used by libkullo. */
public abstract class Registry {
    /**
     * Sets the log listener. If null is passed, default behavior is restored,
     * which means writing the log messages to stdout.
     */
    public static native void setLogListener(@Nullable LogListener listener);

    /** Sets a new TaskRunner. Must be done before any async method is called. */
    public static native void setTaskRunner(@NonNull TaskRunner taskRunner);

    private static final class CppProxy extends Registry
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
    }
}
