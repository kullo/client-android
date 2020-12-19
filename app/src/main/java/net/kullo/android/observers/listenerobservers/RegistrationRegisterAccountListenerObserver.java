/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
