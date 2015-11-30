/* Copyright 2015 Kullo GmbH. All rights reserved. */
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
