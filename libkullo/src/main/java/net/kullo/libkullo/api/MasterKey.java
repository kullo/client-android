/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.libkullo.api;

import android.support.annotation.NonNull;

import java.util.ArrayList;

public class MasterKey extends MasterKeyBase {

    public MasterKey(@NonNull ArrayList<String> blocks) {
        super(blocks);

        if (!InternalMasterKeyUtils.isValid(blocks)) {
            throw new IllegalArgumentException("The argument doesn't form a valid MasterKey");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof MasterKey)) return false;

        return blocks.equals(((MasterKey) other).blocks);
    }

    @Override
    public int hashCode() {
        return blocks.hashCode();
    }
}
