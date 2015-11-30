// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class InternalDateTimeUtils {
    /** Checks a set of DateTime constructor arguments for validity */
    public static native boolean isValid(short year, byte month, byte day, byte hour, byte minute, byte second, short tzOffsetMinutes);

    /** Returns the current time in the UTC timezone */
    public static native DateTime nowUtc();

    /**
     * Returns the RFC3339 representation
     * yyyy-mm-ddThh:mm:ss[.f+](Z|(+|-)hh:mm) (case-insensitive)
     */
    public static native String toString(DateTime dateTime);

    /**
     * Compares two DateTime objects, taking time zones etc. into account.
     * Returns -1 iff lhs < rhs, 0 iff lhs == rhs, 1 iff lhs > rhs.
     */
    public static native byte compare(DateTime lhs, DateTime rhs);

    private static final class CppProxy extends InternalDateTimeUtils
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
