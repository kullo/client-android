/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.libkullo.api;

import android.support.annotation.NonNull;

public class Address extends AddressBase {

    public Address(@NonNull String localPart, @NonNull String domainPart) {
        super(localPart, domainPart);

        if (!InternalAddressUtils.isValid(localPart, domainPart)) {
            throw new IllegalArgumentException("The arguments don't form a valid Address");
        }
    }

    @Override
    public String toString() {
        return localPart + "#" + domainPart;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Address)) return false;

        final Address otherAddress = (Address) other;
        return localPart.equals(otherAddress.localPart)
            && domainPart.equals(otherAddress.domainPart);
    }

    @Override
    public int hashCode() {
        return localPart.hashCode() ^ domainPart.hashCode();
    }
}
