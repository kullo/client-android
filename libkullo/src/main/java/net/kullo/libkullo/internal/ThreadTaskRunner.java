/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.libkullo.internal;

import android.util.Log;

import net.kullo.libkullo.api.Task;
import net.kullo.libkullo.api.TaskRunner;

public class ThreadTaskRunner extends TaskRunner {
    @Override
    public void runTaskAsync(final Task task) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        }).start();
    }
}
