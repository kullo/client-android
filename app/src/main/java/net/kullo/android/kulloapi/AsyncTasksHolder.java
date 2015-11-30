/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.AsyncTask;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds tasks to prevent GC from destroying them before they are done
 */
public class AsyncTasksHolder {
    private List<AsyncTask> mContainer = new LinkedList<AsyncTask>();

    void add(AsyncTask newTask) {
        cleanup();
        mContainer.add(newTask);
    }

    private void cleanup() {
        Iterator<AsyncTask> itr = mContainer.iterator();
        while (itr.hasNext()) {
            AsyncTask o = itr.next();

            if (o.isDone()) {
                itr.remove();
            }
        }
    }
}
