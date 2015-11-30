/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.android.observers.listenerobservers.SyncerRunListenerObserver;
import net.kullo.libkullo.api.SyncMode;

public class SyncManager {
    private AsyncTasksHolder mAsyncTasksHolder = new AsyncTasksHolder();
    private SyncerRunListenerObserver mSyncerRunListenerObserver = null;

    private boolean mBusy = false;
    private boolean mSendSyncWaiting = false;
    private boolean mFullSyncWaiting = false;

    public SyncManager() {
    }

    public void triggerSync(SyncMode mode) {
        // first run?
        if (mSyncerRunListenerObserver == null) {
            registerSyncFinishedListenerObserver();
        }

        // if nothing is running, just launch. Otherwise set waiting
        if (!mBusy) {
            mBusy = true;
            mAsyncTasksHolder.add(KulloConnector.get().startSyncTask(mode));
        } else {
            // Set waiting flag for the incoming task
            mFullSyncWaiting = mFullSyncWaiting | (mode == SyncMode.WITHOUTATTACHMENTS);
            mSendSyncWaiting = mSendSyncWaiting | (mode == SyncMode.SENDONLY);
        }
    }

    private void syncFinished() {
        mBusy = false;

        // check if anything waiting, then launch

        if (mFullSyncWaiting) {
            // Reset mSendSyncWaiting as well because full sync will handle the sending
            mFullSyncWaiting = false;
            mSendSyncWaiting = false;
            triggerSync(SyncMode.WITHOUTATTACHMENTS);
            return;
        }

        // if there was no "general"
        if (mSendSyncWaiting) {
            mSendSyncWaiting = false;
            triggerSync(SyncMode.SENDONLY);
            return;
        }

        // nothing waiting
    }

    private void registerSyncFinishedListenerObserver() {
        mSyncerRunListenerObserver = new SyncerRunListenerObserver() {
            @Override
            public void draftAttachmentsTooBig(long convId) {
                syncFinished();
            }

            @Override
            public void finished() {
                syncFinished();
            }

            @Override
            public void error(String error) {
                syncFinished();
            }
        };

        KulloConnector.get().addListenerObserver(
                SyncerRunListenerObserver.class,
                mSyncerRunListenerObserver);
    }

    public boolean isBusy() {
        return mBusy;
    }
}
