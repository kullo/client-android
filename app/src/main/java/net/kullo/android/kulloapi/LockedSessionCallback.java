/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.Session;

public interface LockedSessionCallback {
    public void run(Session lockedSession);
}
