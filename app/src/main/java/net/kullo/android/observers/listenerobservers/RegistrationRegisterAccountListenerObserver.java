/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AddressNotAvailableReason;
import net.kullo.libkullo.api.Challenge;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.NetworkError;

public interface RegistrationRegisterAccountListenerObserver extends ListenerObserver {
    void challengeNeeded(String address, Challenge challenge);
    void addressNotAvailable(String address, AddressNotAvailableReason reason);
    void finished(Address address, MasterKey masterKeyAsPem);
    void error(Address address, NetworkError error);
}
