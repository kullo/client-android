/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.libkullo;

import net.kullo.libkullo.api.Registry;
import net.kullo.libkullo.internal.AndroidLogListener;
import net.kullo.libkullo.internal.ThreadTaskRunner;
import net.kullo.libkullo.internal.okhttp.HttpClientFactoryImpl;

public final class LibKullo {
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("kullo");
    }

    private LibKullo() {}

    private static boolean isInitialized = false;

    /** Call this method before using anything from libkullo */
    public static void init() {
        if (!isInitialized)
        {
            Registry.setLogListener(new AndroidLogListener());
            Registry.setTaskRunner(new ThreadTaskRunner());

            net.kullo.libkullo.http.Registry.setHttpClientFactory(new HttpClientFactoryImpl());

            isInitialized = true;
        }
    }
}
