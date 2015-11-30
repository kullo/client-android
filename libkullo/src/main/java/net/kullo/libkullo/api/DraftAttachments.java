// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DraftAttachments {
    /** Returns all attachments for the given draft */
    public abstract ArrayList<Long> allForDraft(long convId);

    /** Adds a new attachment to a draft */
    public abstract AsyncTask addAsync(long convId, String path, String mimeType, DraftAttachmentsAddListener listener);

    /** Removes an attachment from a draft */
    public abstract void remove(long convId, long attId);

    public abstract String filename(long convId, long attId);

    public abstract void setFilename(long convId, long attId, String filename);

    public abstract String mimeType(long convId, long attId);

    public abstract long size(long convId, long attId);

    public abstract String hash(long convId, long attId);

    /** Gets the content of the attachment as a BLOB */
    public abstract AsyncTask contentAsync(long convId, long attId, DraftAttachmentsContentListener listener);

    /**
     * Saves the content of the attachment to a file. Path contains the absolute
     * path where the file should be saved, including the filename.
     */
    public abstract AsyncTask saveToAsync(long convId, long attId, String path, DraftAttachmentsSaveToListener listener);

    private static final class CppProxy extends DraftAttachments
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
        public ArrayList<Long> allForDraft(long convId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_allForDraft(this.nativeRef, convId);
        }
        private native ArrayList<Long> native_allForDraft(long _nativeRef, long convId);

        @Override
        public AsyncTask addAsync(long convId, String path, String mimeType, DraftAttachmentsAddListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_addAsync(this.nativeRef, convId, path, mimeType, listener);
        }
        private native AsyncTask native_addAsync(long _nativeRef, long convId, String path, String mimeType, DraftAttachmentsAddListener listener);

        @Override
        public void remove(long convId, long attId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_remove(this.nativeRef, convId, attId);
        }
        private native void native_remove(long _nativeRef, long convId, long attId);

        @Override
        public String filename(long convId, long attId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_filename(this.nativeRef, convId, attId);
        }
        private native String native_filename(long _nativeRef, long convId, long attId);

        @Override
        public void setFilename(long convId, long attId, String filename)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setFilename(this.nativeRef, convId, attId, filename);
        }
        private native void native_setFilename(long _nativeRef, long convId, long attId, String filename);

        @Override
        public String mimeType(long convId, long attId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_mimeType(this.nativeRef, convId, attId);
        }
        private native String native_mimeType(long _nativeRef, long convId, long attId);

        @Override
        public long size(long convId, long attId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_size(this.nativeRef, convId, attId);
        }
        private native long native_size(long _nativeRef, long convId, long attId);

        @Override
        public String hash(long convId, long attId)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_hash(this.nativeRef, convId, attId);
        }
        private native String native_hash(long _nativeRef, long convId, long attId);

        @Override
        public AsyncTask contentAsync(long convId, long attId, DraftAttachmentsContentListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_contentAsync(this.nativeRef, convId, attId, listener);
        }
        private native AsyncTask native_contentAsync(long _nativeRef, long convId, long attId, DraftAttachmentsContentListener listener);

        @Override
        public AsyncTask saveToAsync(long convId, long attId, String path, DraftAttachmentsSaveToListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_saveToAsync(this.nativeRef, convId, attId, path, listener);
        }
        private native AsyncTask native_saveToAsync(long _nativeRef, long convId, long attId, String path, DraftAttachmentsSaveToListener listener);
    }
}
