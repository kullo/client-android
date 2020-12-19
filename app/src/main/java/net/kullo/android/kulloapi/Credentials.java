/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.MasterKey;

public class Credentials {
    public Credentials(Address address, MasterKey masterKey) {
        RuntimeAssertion.require(address != null);
        RuntimeAssertion.require(masterKey != null);

        this.address = address;
        this.masterKey = masterKey;
    }

    public Address getAddress() {
        return address;
    }

    public MasterKey getMasterKey() {
        return masterKey;
    }

    private Address address;
    private MasterKey masterKey;
}
