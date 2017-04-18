// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from messages.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

/** Delivery information for a single recipient. */
public abstract class Delivery {
    public abstract Address recipient();

    public abstract DeliveryState state();

    public abstract DeliveryReason reason();

    public abstract DateTime date();

    private static final class CppProxy extends Delivery
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
        public Address recipient()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_recipient(this.nativeRef);
        }
        private native Address native_recipient(long _nativeRef);

        @Override
        public DeliveryState state()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_state(this.nativeRef);
        }
        private native DeliveryState native_state(long _nativeRef);

        @Override
        public DeliveryReason reason()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_reason(this.nativeRef);
        }
        private native DeliveryReason native_reason(long _nativeRef);

        @Override
        public DateTime date()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_date(this.nativeRef);
        }
        private native DateTime native_date(long _nativeRef);
    }
}