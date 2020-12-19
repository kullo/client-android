/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.Session;

public interface LockedSessionCallback {
    public void run(Session lockedSession);
}
