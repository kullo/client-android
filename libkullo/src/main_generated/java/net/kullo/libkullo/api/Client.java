// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from client.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** A Client is the entry point to most of libkullo. */
public abstract class Client {
    /** Constants for the use with versions() */
    @NonNull
    public static final String BOTAN = "Botan";

    @NonNull
    public static final String BOOST = "Boost";

    @NonNull
    public static final String JSONCPP = "JsonCpp";

    @NonNull
    public static final String LIBKULLO = "libkullo";

    @NonNull
    public static final String SMARTSQLITE = "SmartSqlite";

    @NonNull
    public static final String SQLITE = "SQLite";

    /**
     * Opens a session for the given user. dbFilePath is the absolute path to the
     * database file, including the filename. The folder must exist, and it must
     * be possible for the current user to create files in it. The file itself is
     * created if it doesn't already exist.
     *
     * Attention: Don't ever re-use the same DB file for multiple accounts!
     */
    @NonNull
    public abstract AsyncTask createSessionAsync(@NonNull Address address, @NonNull MasterKey masterKey, @NonNull String dbFilePath, @NonNull SessionListener sessionListener, @NonNull ClientCreateSessionListener listener);

    /** Check whether an address exists. */
    @NonNull
    public abstract AsyncTask addressExistsAsync(@NonNull Address address, @NonNull ClientAddressExistsListener listener);

    /** Check whether the master key is valid for the given address. */
    @NonNull
    public abstract AsyncTask checkCredentialsAsync(@NonNull Address address, @NonNull MasterKey masterKey, @NonNull ClientCheckCredentialsListener listener);

    /** Generate new keys, which is the first step to registering an account. */
    @NonNull
    public abstract AsyncTask generateKeysAsync(@NonNull ClientGenerateKeysListener listener);

    /** Returns pairs of <library name, version number> for the libraries used. */
    @NonNull
    public abstract HashMap<String, String> versions();

    /** Create a new Client instance. You will most probably only need one. */
    @NonNull
    public static native Client create();

    private static final class CppProxy extends Client
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
        public AsyncTask createSessionAsync(Address address, MasterKey masterKey, String dbFilePath, SessionListener sessionListener, ClientCreateSessionListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_createSessionAsync(this.nativeRef, address, masterKey, dbFilePath, sessionListener, listener);
        }
        private native AsyncTask native_createSessionAsync(long _nativeRef, Address address, MasterKey masterKey, String dbFilePath, SessionListener sessionListener, ClientCreateSessionListener listener);

        @Override
        public AsyncTask addressExistsAsync(Address address, ClientAddressExistsListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_addressExistsAsync(this.nativeRef, address, listener);
        }
        private native AsyncTask native_addressExistsAsync(long _nativeRef, Address address, ClientAddressExistsListener listener);

        @Override
        public AsyncTask checkCredentialsAsync(Address address, MasterKey masterKey, ClientCheckCredentialsListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_checkCredentialsAsync(this.nativeRef, address, masterKey, listener);
        }
        private native AsyncTask native_checkCredentialsAsync(long _nativeRef, Address address, MasterKey masterKey, ClientCheckCredentialsListener listener);

        @Override
        public AsyncTask generateKeysAsync(ClientGenerateKeysListener listener)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_generateKeysAsync(this.nativeRef, listener);
        }
        private native AsyncTask native_generateKeysAsync(long _nativeRef, ClientGenerateKeysListener listener);

        @Override
        public HashMap<String, String> versions()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_versions(this.nativeRef);
        }
        private native HashMap<String, String> native_versions(long _nativeRef);
    }
}
