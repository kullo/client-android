/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A container that contains 0 or 1 non-null elements. Similar to an Java 8 Optional
 *
 * An instance of this container can be final but the object inside can be exchanged.
 *
 * @param <T>
 */
public class ObjectContainer<T> {

    @Nullable private T object;

    public void set(@NonNull T newObject) {
        object = newObject;
    }

    @NonNull
    public T get() throws ObjectNotSet {
        if (object==null) {
            throw new ObjectNotSet();
        }

        return object;
    }
}
