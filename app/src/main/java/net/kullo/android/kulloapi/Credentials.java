/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
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
