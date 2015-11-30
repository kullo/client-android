// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Drafts {
    public abstract String text(long convId);

    public abstract void setText(long convId, String text);

    public abstract DraftState state(long convId);

    /**
     * The draft will be sent on the next sync. This will throw an exception if
     * UserSettings::name() is empty.
     */
    public abstract void prepareToSend(long convId);

    /** Clears the draft and removes the corresponding attachments. */
    public abstract void clear(long convId);

    private static final class CppProxy extends Drafts
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
        public String text(long convId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_text(this.nativeRef, convId);
        }
        private native String native_text(long _nativeRef, long convId);

        @Override
        public void setText(long convId, String text)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setText(this.nativeRef, convId, text);
        }
        private native void native_setText(long _nativeRef, long convId, String text);

        @Override
        public DraftState state(long convId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_state(this.nativeRef, convId);
        }
        private native DraftState native_state(long _nativeRef, long convId);

        @Override
        public void prepareToSend(long convId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_prepareToSend(this.nativeRef, convId);
        }
        private native void native_prepareToSend(long _nativeRef, long convId);

        @Override
        public void clear(long convId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_clear(this.nativeRef, convId);
        }
        private native void native_clear(long _nativeRef, long convId);
    }
}
