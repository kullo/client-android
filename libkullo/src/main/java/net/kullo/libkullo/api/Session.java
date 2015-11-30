// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Session {
    public abstract UserSettings userSettings();

    public abstract Conversations conversations();

    public abstract Messages messages();

    public abstract MessageAttachments messageAttachments();

    public abstract Senders senders();

    public abstract Drafts drafts();

    public abstract DraftAttachments draftAttachments();

    public abstract Syncer syncer();

    public abstract AsyncTask accountInfoAsync(SessionAccountInfoListener listener);

    /** Notify the session of events. Only call this from the UI thread! */
    public abstract ArrayList<Event> notify(InternalEvent internalEvent);

    private static final class CppProxy extends Session
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
        public UserSettings userSettings()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_userSettings(this.nativeRef);
        }
        private native UserSettings native_userSettings(long _nativeRef);

        @Override
        public Conversations conversations()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_conversations(this.nativeRef);
        }
        private native Conversations native_conversations(long _nativeRef);

        @Override
        public Messages messages()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_messages(this.nativeRef);
        }
        private native Messages native_messages(long _nativeRef);

        @Override
        public MessageAttachments messageAttachments()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_messageAttachments(this.nativeRef);
        }
        private native MessageAttachments native_messageAttachments(long _nativeRef);

        @Override
        public Senders senders()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_senders(this.nativeRef);
        }
        private native Senders native_senders(long _nativeRef);

        @Override
        public Drafts drafts()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_drafts(this.nativeRef);
        }
        private native Drafts native_drafts(long _nativeRef);

        @Override
        public DraftAttachments draftAttachments()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_draftAttachments(this.nativeRef);
        }
        private native DraftAttachments native_draftAttachments(long _nativeRef);

        @Override
        public Syncer syncer()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_syncer(this.nativeRef);
        }
        private native Syncer native_syncer(long _nativeRef);

        @Override
        public AsyncTask accountInfoAsync(SessionAccountInfoListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_accountInfoAsync(this.nativeRef, listener);
        }
        private native AsyncTask native_accountInfoAsync(long _nativeRef, SessionAccountInfoListener listener);

        @Override
        public ArrayList<Event> notify(InternalEvent internalEvent)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_notify(this.nativeRef, internalEvent);
        }
        private native ArrayList<Event> native_notify(long _nativeRef, InternalEvent internalEvent);
    }
}
