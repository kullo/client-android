/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import android.util.Log;

import net.kullo.libkullo.api.AsyncTask;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds tasks to prevent GC from destroying them before they are done
 *
 * All methods are thread-safe.
 */
public class AsyncTasksHolder {
    private final String TAG = "AsyncTasksHolder";
    private final List<AsyncTask> mContainer = new LinkedList<AsyncTask>();

    // Adds a task to the holder if it is not yet contained
    void add(AsyncTask newTask) {
        cleanup();

        synchronized(mContainer) {
            if (!mContainer.contains(newTask)) {
                mContainer.add(newTask);
            }
        }
    }

    public void cancelAll() {
        cleanup();

        synchronized (mContainer) {
            Log.d(TAG, "Canceling " + mContainer.size() + " tasks ...");
            for (AsyncTask task : mContainer) {
                task.cancel();
            }
        }
    }

    public void waitUntilAllDone() {
        cleanup();

        synchronized (mContainer) {
            Log.d(TAG, "Waiting for " + mContainer.size() + " tasks to be done ...");
            for (AsyncTask task : mContainer) {
                task.waitUntilDone();
            }
        }
    }

    private void cleanup() {
        synchronized (mContainer) {
            Iterator<AsyncTask> itr = mContainer.iterator();
            while (itr.hasNext()) {
                AsyncTask o = itr.next();

                if (o.isDone()) {
                    itr.remove();
                }
            }
        }
    }
}
