// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from session.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** Information about a Kullo account */
public final class AccountInfo {


    /*package*/ final String planName;

    /*package*/ final Long storageQuota;

    /*package*/ final Long storageUsed;

    /*package*/ final String settingsUrl;

    public AccountInfo(
            @Nullable String planName,
            @Nullable Long storageQuota,
            @Nullable Long storageUsed,
            @Nullable String settingsUrl) {
        this.planName = planName;
        this.storageQuota = storageQuota;
        this.storageUsed = storageUsed;
        this.settingsUrl = settingsUrl;
    }

    @Nullable
    public String getPlanName() {
        return planName;
    }

    /** in bytes */
    @Nullable
    public Long getStorageQuota() {
        return storageQuota;
    }

    @Nullable
    public Long getStorageUsed() {
        return storageUsed;
    }

    /**
     * URL to web interface where account settings (notifications, ...) can be
     * configured
     */
    @Nullable
    public String getSettingsUrl() {
        return settingsUrl;
    }

    @Override
    public String toString() {
        return "AccountInfo{" +
                "planName=" + planName +
                "," + "storageQuota=" + storageQuota +
                "," + "storageUsed=" + storageUsed +
                "," + "settingsUrl=" + settingsUrl +
        "}";
    }

}
