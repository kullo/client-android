// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from usersettings.djinni

package net.kullo.libkullo.api;

import java.util.concurrent.atomic.AtomicBoolean;

/** Settings specific to the local user */
public abstract class UserSettings {
    /** Kullo address (e.g. "john.doe#kullo.net") */
    public abstract Address address();

    public abstract MasterKey masterKey();

    /** Real name (e.g. "John Doe"). Defaults to "". */
    public abstract String name();

    public abstract void setName(String name);

    /** Organization (e.g. "Doe Inc."). Defaults to "". */
    public abstract String organization();

    public abstract void setOrganization(String organization);

    /** Message footer (e.g. "42 Doe Ave., Doetown 12345"). Defaults to "". */
    public abstract String footer();

    public abstract void setFooter(String footer);

    /** MIME type of the avatar (e.g. "image/jpeg"). Defaults to "". */
    public abstract String avatarMimeType();

    public abstract void setAvatarMimeType(String mimeType);

    /**
     * Binary contents of the avatar image file of type avatarMimeType.
     * Defaults to a zero-length vector.
     */
    public abstract byte[] avatar();

    public abstract void setAvatar(byte[] avatar);

    /** Whether the masterKey has been backed up by the user. Defaults to false. */
    public abstract boolean keyBackupConfirmed();

    /** Sets keyBackupConfirmed to true and nulls keyBackupDontRemindBefore. */
    public abstract void setKeyBackupConfirmed();

    /**
     * When to show the next backup reminder. Returns null if no reminder date
     * is set. Defaults to a date in the past.
     */
    public abstract DateTime keyBackupDontRemindBefore();

    public abstract void setKeyBackupDontRemindBefore(DateTime dontRemindBefore);

    public static native UserSettings create(Address address, MasterKey masterKey);

    private static final class CppProxy extends UserSettings
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
        public Address address()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_address(this.nativeRef);
        }
        private native Address native_address(long _nativeRef);

        @Override
        public MasterKey masterKey()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_masterKey(this.nativeRef);
        }
        private native MasterKey native_masterKey(long _nativeRef);

        @Override
        public String name()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_name(this.nativeRef);
        }
        private native String native_name(long _nativeRef);

        @Override
        public void setName(String name)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setName(this.nativeRef, name);
        }
        private native void native_setName(long _nativeRef, String name);

        @Override
        public String organization()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_organization(this.nativeRef);
        }
        private native String native_organization(long _nativeRef);

        @Override
        public void setOrganization(String organization)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setOrganization(this.nativeRef, organization);
        }
        private native void native_setOrganization(long _nativeRef, String organization);

        @Override
        public String footer()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_footer(this.nativeRef);
        }
        private native String native_footer(long _nativeRef);

        @Override
        public void setFooter(String footer)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setFooter(this.nativeRef, footer);
        }
        private native void native_setFooter(long _nativeRef, String footer);

        @Override
        public String avatarMimeType()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_avatarMimeType(this.nativeRef);
        }
        private native String native_avatarMimeType(long _nativeRef);

        @Override
        public void setAvatarMimeType(String mimeType)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setAvatarMimeType(this.nativeRef, mimeType);
        }
        private native void native_setAvatarMimeType(long _nativeRef, String mimeType);

        @Override
        public byte[] avatar()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_avatar(this.nativeRef);
        }
        private native byte[] native_avatar(long _nativeRef);

        @Override
        public void setAvatar(byte[] avatar)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setAvatar(this.nativeRef, avatar);
        }
        private native void native_setAvatar(long _nativeRef, byte[] avatar);

        @Override
        public boolean keyBackupConfirmed()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_keyBackupConfirmed(this.nativeRef);
        }
        private native boolean native_keyBackupConfirmed(long _nativeRef);

        @Override
        public void setKeyBackupConfirmed()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setKeyBackupConfirmed(this.nativeRef);
        }
        private native void native_setKeyBackupConfirmed(long _nativeRef);

        @Override
        public DateTime keyBackupDontRemindBefore()
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            return native_keyBackupDontRemindBefore(this.nativeRef);
        }
        private native DateTime native_keyBackupDontRemindBefore(long _nativeRef);

        @Override
        public void setKeyBackupDontRemindBefore(DateTime dontRemindBefore)
        {
            assert !this.destroyed.get() : "trying to use a destroyed object";
            native_setKeyBackupDontRemindBefore(this.nativeRef, dontRemindBefore);
        }
        private native void native_setKeyBackupDontRemindBefore(long _nativeRef, DateTime dontRemindBefore);
    }
}
