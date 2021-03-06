/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.observers.EventObserver;
import net.kullo.android.observers.ListenerObserver;
import net.kullo.android.observers.eventobservers.ConversationsEventObserver;
import net.kullo.android.observers.eventobservers.DraftAttachmentAddedEventObserver;
import net.kullo.android.observers.eventobservers.DraftAttachmentRemovedEventObserver;
import net.kullo.android.observers.eventobservers.DraftEventObserver;
import net.kullo.android.observers.eventobservers.MessageAddedEventObserver;
import net.kullo.android.observers.eventobservers.MessageAttachmentsDownloadedChangedEventObserver;
import net.kullo.android.observers.eventobservers.MessageRemovedEventObserver;
import net.kullo.android.observers.eventobservers.MessageStateEventObserver;
import net.kullo.android.observers.listenerobservers.ClientCheckCredentialsListenerObserver;
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.android.observers.listenerobservers.ClientGenerateKeysListenerObserver;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsAddListenerObserver;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsSaveListenerObserver;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;
import net.kullo.android.observers.listenerobservers.RegistrationRegisterAccountListenerObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.LoginActivity;
import net.kullo.android.screens.WelcomeActivity;
import net.kullo.android.storage.AppPreferences;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AccountInfo;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AddressHelpers;
import net.kullo.libkullo.api.AddressNotAvailableReason;
import net.kullo.libkullo.api.AsyncTask;
import net.kullo.libkullo.api.Challenge;
import net.kullo.libkullo.api.Client;
import net.kullo.libkullo.api.ClientCheckCredentialsListener;
import net.kullo.libkullo.api.ClientCreateSessionListener;
import net.kullo.libkullo.api.ClientGenerateKeysListener;
import net.kullo.libkullo.api.Conversations;
import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.DraftAttachmentsAddListener;
import net.kullo.libkullo.api.DraftAttachmentsSaveToListener;
import net.kullo.libkullo.api.DraftPart;
import net.kullo.libkullo.api.DraftState;
import net.kullo.libkullo.api.Event;
import net.kullo.libkullo.api.EventType;
import net.kullo.libkullo.api.InternalEvent;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.MasterKeyHelpers;
import net.kullo.libkullo.api.MessageAttachmentsContentListener;
import net.kullo.libkullo.api.MessageAttachmentsSaveToListener;
import net.kullo.libkullo.api.MessagesSearchListener;
import net.kullo.libkullo.api.MessagesSearchResult;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.PushToken;
import net.kullo.libkullo.api.PushTokenEnvironment;
import net.kullo.libkullo.api.PushTokenType;
import net.kullo.libkullo.api.Registration;
import net.kullo.libkullo.api.RegistrationRegisterAccountListener;
import net.kullo.libkullo.api.SearchPredicateOperator;
import net.kullo.libkullo.api.SenderPredicate;
import net.kullo.libkullo.api.Session;
import net.kullo.libkullo.api.SessionAccountInfoListener;
import net.kullo.libkullo.api.SessionListener;
import net.kullo.libkullo.api.SyncMode;
import net.kullo.libkullo.api.SyncProgress;
import net.kullo.libkullo.api.SyncerListener;
import net.kullo.libkullo.api.UserSettings;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Seconds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionConnector {
    private static final String TAG = "SessionConnector";
    private static final int SECONDS_BETWEEN_SYNCS = 5 * 60;

    private static final SessionConnector SINGLETON = new SessionConnector();
    @AnyThread
    @NonNull
    public static SessionConnector get() {
        synchronized (SINGLETON) {
            return SINGLETON;
        }
    }

    // Access to mSession must be synchronized, since some functions like
    // sessionAvailable() are called from other threads than main. Use extra final
    // field since mSession can change during the lifetime of SessionConnector
    // Note that libkullo functions must be called from the main thread.
    private final Boolean mSessionGuard = false;
    private Session mSession = null;

    @Nullable private PushToken mPushToken = null;
    private Registration mRegistration;

    // thread safe implementation
    final private AsyncTasksHolder mTaskHolder = new AsyncTasksHolder();

    // thread safe implementation
    final private ConcurrentHashMap<Class, LinkedList<ListenerObserver>> mListenerObservers;
    // only used on main thread
    final private HashMap<Class, LinkedList<EventObserver>> mEventObservers;

    private SessionConnector() {
        mListenerObservers = new ConcurrentHashMap<>(10);
        mListenerObservers.put(ClientCheckCredentialsListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(ClientCreateSessionListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(ClientGenerateKeysListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(DraftAttachmentsAddListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(DraftAttachmentsSaveListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(MessageAttachmentsSaveListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(RegistrationRegisterAccountListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(SyncerListenerObserver.class, new LinkedList<ListenerObserver>());

        mEventObservers = new HashMap<>(10);
        mEventObservers.put(ConversationsEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(DraftEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(DraftAttachmentAddedEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(DraftAttachmentRemovedEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(MessageAddedEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(MessageRemovedEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(MessageStateEventObserver.class, new LinkedList<EventObserver>());
        mEventObservers.put(MessageAttachmentsDownloadedChangedEventObserver.class, new LinkedList<EventObserver>());

        setupInternalEventObservers();
    }

    private void setupInternalEventObservers() {
        addEventObserver(MessageAddedEventObserver.class, new MessageAddedEventObserver() {
            @Override
            public void messageAdded(long conversationId, long messageId) {
                updateBadgeCount();
            }
        });
        addEventObserver(MessageStateEventObserver.class, new MessageStateEventObserver() {
            @Override
            public void messageStateChanged(long conversationId, long messageId) {
                updateBadgeCount();
            }
        });
        addEventObserver(MessageRemovedEventObserver.class, new MessageRemovedEventObserver() {
            @Override
            public void messageRemoved(long conversationId, long messageId) {
                updateBadgeCount();
            }
        });
    }

    @MainThread
    private void updateBadgeCount() {
        int unreadCount = 0;
        final List<Long> conversations = getAllConversationIds(false);
        for (Long id : conversations) {
            unreadCount += getConversationUnreadCount(id);
        }

        KulloApplication.sharedInstance.handleBadge(unreadCount);
    }

    //LOGIN LOGOUT

    @NonNull
    @MainThread
    public CreateSessionResult createActivityWithSession(Activity callingActivity) {
        if (sessionAvailable()) return new CreateSessionResult(CreateSessionState.EXISTS);

        Log.d(TAG, "No session available. Creating session if credentials are stored ...");
        Credentials credentials = SessionConnector.get().loadStoredCredentials();
        if (credentials == null) {
            Log.d(TAG, "No credentials found. Moving to WelcomeActivity ...");
            callingActivity.startActivity(new Intent(callingActivity, WelcomeActivity.class));
            callingActivity.finish();
            return new CreateSessionResult(CreateSessionState.NO_CREDENTIALS);
        }

        return new CreateSessionResult(CreateSessionState.CREATING,
                SessionConnector.get().createSession(callingActivity, credentials)
        );
    }

    @MainThread
    public void checkLoginAndCreateSession(final LoginActivity callingActivity, final Address address, final MasterKey masterKey) {
        RuntimeAssertion.require(address != null);
        RuntimeAssertion.require(masterKey != null);

        Client client = ClientConnector.get().getClient();

        mTaskHolder.add(client.checkCredentialsAsync(address, masterKey, new ClientCheckCredentialsListener() {
            @Override
            public void finished(@NonNull Address address, @NonNull MasterKey masterKey, boolean valid) {
                if (valid) {
                    KulloApplication.sharedInstance.preferences.set(
                        AppPreferences.AppScopeKey.ACTIVE_USER,
                        address.toString());
                    KulloApplication.sharedInstance.preferences.set(
                        AppPreferences.AppScopeKey.LAST_ACTIVE_USER,
                        address.toString());
                    Credentials credentials = new Credentials(address, masterKey);
                    storeCredentials(callingActivity, credentials);
                    createSession(callingActivity, credentials);
                } else {
                    for (ListenerObserver observer : mListenerObservers.get(ClientCheckCredentialsListenerObserver.class)) {
                        ((ClientCheckCredentialsListenerObserver) observer).loginFailed();
                    }
                }
            }

            @Override
            public void error(@NonNull Address address, @NonNull NetworkError error) {
                for (ListenerObserver observer : mListenerObservers.get(ClientCheckCredentialsListenerObserver.class)) {
                    ((ClientCheckCredentialsListenerObserver) observer).error(error);
                }
            }
        }));
    }

    @MainThread
    public AsyncTask createSession(final Activity callingActivity, Credentials credentials) {
        RuntimeAssertion.require(credentials != null);

        String dbFilePath = KulloUtils.getDatabasePathBase(callingActivity.getApplication(), credentials.getAddress());
        Log.d(TAG, "dbFilePath = " + dbFilePath);

        Client client = ClientConnector.get().getClient();

        // Store task in local member to ensure it is not destroyed. Pass task as return
        // value to caller to enable it to block using AsyncTask#waitUntilDone()
        AsyncTask task = client.createSessionAsync(credentials.getAddress(), credentials.getMasterKey(), dbFilePath, new SessionListener() {
            @Override
            public void internalEvent(@NonNull final InternalEvent event) {
                callingActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSession != null) {
                            ArrayList<Event> events = mSession.notify(event);
                            for (Event e : events) {
                                if (e.getEvent() == EventType.MESSAGEADDED) {
                                    for (EventObserver o : mEventObservers.get(MessageAddedEventObserver.class)) {
                                        ((MessageAddedEventObserver) o).messageAdded(e.getConversationId(), e.getMessageId());
                                    }
                                } else if (e.getEvent() == EventType.MESSAGEREMOVED) {
                                    for (EventObserver o : mEventObservers.get(MessageRemovedEventObserver.class)) {
                                        ((MessageRemovedEventObserver) o).messageRemoved(e.getConversationId(), e.getMessageId());
                                    }
                                } else if (e.getEvent() == EventType.MESSAGESTATECHANGED) {
                                    for (EventObserver o : mEventObservers.get(MessageStateEventObserver.class)) {
                                        ((MessageStateEventObserver) o).messageStateChanged(e.getConversationId(), e.getMessageId());
                                    }
                                } else if (e.getEvent() == EventType.MESSAGEATTACHMENTSDOWNLOADEDCHANGED) {
                                    for (EventObserver o : mEventObservers.get(MessageAttachmentsDownloadedChangedEventObserver.class)) {
                                        ((MessageAttachmentsDownloadedChangedEventObserver) o).messageAttachmentsDownloadedChanged(e.getMessageId());
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONADDED) {
                                    for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                        ((ConversationsEventObserver) o).conversationAdded(e.getConversationId());
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONREMOVED) {
                                    for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                        ((ConversationsEventObserver) o).conversationRemoved(e.getConversationId());
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONCHANGED) {
                                    for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                        ((ConversationsEventObserver) o).conversationChanged(e.getConversationId());
                                    }
                                } else if (e.getEvent() == EventType.DRAFTSTATECHANGED) {
                                    for (EventObserver o : mEventObservers.get(DraftEventObserver.class)) {
                                        ((DraftEventObserver) o).draftStateChanged(e.getConversationId());
                                    }
                                } else if (e.getEvent() == EventType.DRAFTTEXTCHANGED) {
                                    for (EventObserver o : mEventObservers.get(DraftEventObserver.class)) {
                                        ((DraftEventObserver) o).draftTextChanged(e.getConversationId());
                                    }
                                } else if (e.getEvent() == EventType.DRAFTATTACHMENTADDED) {
                                    for (EventObserver o : mEventObservers.get(DraftAttachmentAddedEventObserver.class)) {
                                        ((DraftAttachmentAddedEventObserver) o).draftAttachmentAdded(e.getConversationId(), e.getAttachmentId());
                                    }
                                } else if (e.getEvent() == EventType.DRAFTATTACHMENTREMOVED) {
                                    for (EventObserver o : mEventObservers.get(DraftAttachmentRemovedEventObserver.class)) {
                                        ((DraftAttachmentRemovedEventObserver) o).draftAttachmentRemoved(e.getConversationId(), e.getAttachmentId());
                                    }
                                } else {
                                    Log.w(TAG, "Unhandled event type: " + e.getEvent().toString());
                                }
                            }
                        }
                    }
                });
            }
        }, new ClientCreateSessionListener() {
            private final String TAG = "ClientCreateSessionList"; // max 23 chars

            @AnyThread
            @Override
            public void migrationStarted(@NonNull Address address) {
                for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                    ((ClientCreateSessionListenerObserver) observer).migrationStarted();
                }
            }

            @AnyThread
            @Override
            public void finished(@NonNull final Session session) {
                Log.d(TAG, "createSession finished :)");

                synchronized (mSessionGuard) {
                    mSession = session;
                    mSession.syncer().setListener(new ConnectorSyncerListener());
                }

                callingActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Caution: everything in here might run after task.waitUntilDone()

                        copyTempUserNameAndOrganizationIntoDatabase();

                        for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                            ((ClientCreateSessionListenerObserver) observer).finished();
                        }
                    }
                });
            }

            @AnyThread
            @Override
            public void error(@NonNull Address address, @NonNull LocalError error) {
                Log.d(TAG, address.toString() + " " + error.toString());

                for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                    ((ClientCreateSessionListenerObserver) observer).error(error);
                }
            }
        });

        mTaskHolder.add(task);

        return task;
    }

    public void storeCredentials(final Context context, Credentials credentials) {
        final String addressString = credentials.getAddress().toString();
        final List<String> blocks = credentials.getMasterKey().getBlocks();
        RuntimeAssertion.require(blocks.size() == AppPreferences.MASTER_KEY_KEYS.size());

        for (int index = 0; index < blocks.size(); index++) {
            AppPreferences.UserScopeKey key = AppPreferences.MASTER_KEY_KEYS.get(index);
            KulloApplication.sharedInstance.preferences.set(addressString, key, blocks.get(index));
        }
    }

    @Nullable
    public Credentials loadStoredCredentials() {
        String addressString = KulloApplication.sharedInstance.preferences.get(
            AppPreferences.AppScopeKey.ACTIVE_USER);
        if (addressString == null) return null;

        ArrayList<String> blockList = new ArrayList<>();
        for (AppPreferences.UserScopeKey key : AppPreferences.MASTER_KEY_KEYS) {
            String block = KulloApplication.sharedInstance.preferences.get(addressString, key);
            blockList.add(block != null ? block : "");
        }

        // Check validity of loaded login data
        Address address = AddressHelpers.create(addressString);
        MasterKey masterKey = MasterKeyHelpers.createFromDataBlocks(blockList);
        if (address == null || masterKey == null) {
            return null;
        }

        return new Credentials(address, masterKey);
    }

    @MainThread
    public void copyTempUserNameAndOrganizationIntoDatabase() {
        UserSettings userSettings;
        synchronized (mSessionGuard) {
            userSettings = mSession.userSettings();
        }

        String name = KulloApplication.sharedInstance.preferences.get(AppPreferences.AppScopeKey.NEW_USER_NAME);
        if (name != null) {
            userSettings.setName(name);
            KulloApplication.sharedInstance.preferences.clear(AppPreferences.AppScopeKey.NEW_USER_NAME);
        }

        String organization = KulloApplication.sharedInstance.preferences.get(AppPreferences.AppScopeKey.NEW_USER_ORGANIZATION);
        if (organization != null) {
            userSettings.setOrganization(organization);
            KulloApplication.sharedInstance.preferences.clear(AppPreferences.AppScopeKey.NEW_USER_ORGANIZATION);
        }
    }

    @MainThread
    public boolean userSettingsAreValidForSync() {
        synchronized (mSessionGuard) {
            return !mSession.userSettings().name().isEmpty();
        }
    }

    // Callback runs on main thread
    @SuppressLint("StaticFieldLeak")
    @MainThread
    public void logout(final Activity callingActivity, final Runnable callback) {
        Log.d(TAG, "Logging out user ...");

        if (sessionAvailable()) {
            new android.os.AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    mSession.syncer().cancel();
                    mTaskHolder.cancelAll();
                    mSession.syncer().waitUntilDone();
                    mTaskHolder.waitUntilAllDone();

                    // Since all workers are done, this should be the last reference
                    // of the session.
                    synchronized (mSessionGuard) {
                        mSession = null;
                    }

                    KulloApplication.sharedInstance.preferences.clear(
                        AppPreferences.AppScopeKey.ACTIVE_USER);

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    callback.run();
                }
            }.execute();
        } else {
            callback.run();
        }
    }

    private void forgetUser(final Activity callingActivity, final Address address) {
        // Remove database files
        String dbFileBase = KulloUtils.getDatabasePathBase(callingActivity.getApplication(), address);
        ArrayList<String> filesToDelete = new ArrayList<>(Arrays.asList(
            dbFileBase,
            dbFileBase + "-shm",
            dbFileBase + "-wal"
            ));
        Log.d(TAG, "Trying to delete " + filesToDelete + " ...");

        for (String filePath : filesToDelete) {
            File myFile = new File(filePath);
            if (myFile.exists()) {
                if (!myFile.delete()) {
                    Log.e(TAG, "Error deleting database file: " + filePath);
                }
            } else {
                Log.d(TAG, "Skipping non-existing database file: " + filePath);
            }
        }

        // delete credentials from shared preferences
        SharedPreferences sharedPref = callingActivity.getApplicationContext().getSharedPreferences(
            KulloConstants.PREFS_FILE_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();
    }

    @AnyThread
    public boolean sessionAvailable() {
        synchronized (mSessionGuard) {
            return mSession != null;
        }
    }

    // SYNC

    @AnyThread
    public boolean isSyncing() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.syncer().isSyncing();
        }
    }

    @AnyThread
    public void sync() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.syncer().requestSync(SyncMode.WITHOUTATTACHMENTS);
        }
    }

    @AnyThread
    public void syncIfNecessary() {
        final DateTime lastFullSync;
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            lastFullSync = mSession.syncer().lastFullSync();
        }

        if (lastFullSync == null) {
            sync();
        } else {
            org.joda.time.DateTime last = KulloUtils.convertToJodaTime(lastFullSync);
            org.joda.time.DateTime now = org.joda.time.DateTime.now();
            if (Seconds.secondsBetween(last, now).getSeconds() > SECONDS_BETWEEN_SYNCS) {
                sync();
            }
        }
    }

    @AnyThread
    public void sendMessages() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.syncer().requestSync(SyncMode.SENDONLY);
        }
    }

    @AnyThread
    public void runWithLockedSession(@NonNull final LockedSessionCallback lockedSessionCallback) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            lockedSessionCallback.run(mSession);
        }
    }

    @MainThread
    public List<String> getKnownAddressesAsString() {
        AddressSet addresses = new AddressSet();
        List<Long> conversationIds = getAllConversationIds(false);
        for (long conversationId : conversationIds) {
            addresses.addAll(getParticipantAddresses(conversationId));
        }

        List<String> out = new ArrayList<>(addresses.size());
        for (Address address : addresses.sorted()) {
            out.add(address.toString());
        }
        return out;
    }

    public interface SearchCallback {
        @MainThread
        void finished(ArrayList<MessagesSearchResult> results);
    }

    public enum MessageDirection {
        ALL,
        INCOMING,
        OUTGOING,
    }

    @MainThread
    public void search(@NonNull final String query, @NonNull final MessageDirection direction, long conversationId, @NonNull final SearchCallback callback) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);

            SenderPredicate senderPredicate;
            switch (direction) {
                case ALL:
                    senderPredicate = null;
                    break;
                case INCOMING:
                    senderPredicate = new SenderPredicate(
                        SearchPredicateOperator.ISNOT,
                        mSession.userSettings().address().toString());
                    break;
                case OUTGOING:
                    senderPredicate = new SenderPredicate(
                        SearchPredicateOperator.IS,
                        mSession.userSettings().address().toString());
                    break;
                default:
                    senderPredicate = null;
                    RuntimeAssertion.fail();
            }

            mTaskHolder.add(mSession.messages().searchAsync(query, conversationId, senderPredicate, 100, null, new MessagesSearchListener() {
                @Override
                public void finished(@NonNull final ArrayList<MessagesSearchResult> results) {
                    new Handler(KulloApplication.sharedInstance.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.finished(results);
                        }
                    });
                }
            }));
        }
    }

    public interface GetAccountInfoCallback {
        void onDone(@Nullable final AccountInfo accountInfo);
    }

    @MainThread
    public void getAccountInfo(@NonNull final GetAccountInfoCallback callback) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mTaskHolder.add(mSession.accountInfoAsync(new SessionAccountInfoListener() {
                @Override
                public void finished(@NonNull AccountInfo accountInfo) {
                    callback.onDone(accountInfo);
                }

                @Override
                public void error(@NonNull NetworkError error) {
                    callback.onDone(null);
                }
            }));
        }
    }

    private class ConnectorSyncerListener extends SyncerListener {
        @Override
        public void started() {
            for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                ((SyncerListenerObserver) observer).started();
            }
        }

        @Override
        public void draftPartTooBig(long convId, @NonNull DraftPart part, long currentSize, long maxSize) {
            for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                //TODO generalize to pass on part
                ((SyncerListenerObserver) observer).draftAttachmentsTooBig(convId);
            }
        }

        @Override
        public void progressed(@NonNull SyncProgress progress) {
            for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                ((SyncerListenerObserver) observer).progressed(progress);
            }
        }

        @Override
        public void finished() {
            for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                ((SyncerListenerObserver) observer).finished();
            }
        }

        @Override
        public void error(@NonNull NetworkError error) {
            for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                ((SyncerListenerObserver) observer).error(error);
            }
        }
    }

    // CONVERSATIONS

    @NonNull
    @MainThread
    public List<Long> getAllConversationIds(boolean sorted) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);

            ArrayList<Long> allConversationIds = mSession.conversations().all();
            if (sorted) {
                CountingComparator comparator = new ConversationsComparatorDsc(mSession);
                KulloSort.sort(allConversationIds, comparator);
                Log.d(TAG, "Done sorting conversations. " + comparator.getStats());
            }

            return allConversationIds;
        }
    }

    @NonNull
    @MainThread
    public org.joda.time.DateTime getLatestMessageTimestamp(long conversationId) {
        final DateTime latestMessageTimestamp;
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            latestMessageTimestamp = mSession.conversations().latestMessageTimestamp(conversationId);
        }
        return KulloUtils.convertToJodaTime(latestMessageTimestamp);
    }

    @NonNull
    public org.joda.time.DateTime emptyConversationTimestamp() {
        return KulloUtils.convertToJodaTime(Conversations.emptyConversationTimestamp());
    }

    @MainThread
    public void removeConversations(@NonNull final List<Long> conversationIds) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);

            for (long conversationId : conversationIds) {
                mSession.conversations().triggerRemoval(conversationId);
            }
        }
    }

    // Get all data at once to avoid unnecessary JNI calls when scrolling the list
    @MainThread
    public ConversationData getConversationData(Context context, long conversationId) {
        RuntimeAssertion.require(mSession != null);

        ConversationData out = new ConversationData();
        out.participants = getParticipantAddresses(conversationId);

        for (Address address : out.participants.sorted()) {
            long latestMessageId = mSession.messages().latestForSender(address);
            final String participantName = latestMessageId != -1
                ? mSession.senders().name(latestMessageId) : "";
            final String participantOrganization = latestMessageId != -1
                ? mSession.senders().organization(latestMessageId) : "";

            // Name and Organization
            out.participantsName.put(address.toString(), participantName);
            out.participantsOrganization.put(address.toString(), participantOrganization);

            // Title
            String participantTitle = !participantName.isEmpty() ? participantName : address.toString();
            out.participantsTitle.add(participantTitle);

            // Avatar
            Bitmap participantAvatar = null;
            if (latestMessageId != -1) {
                participantAvatar = AvatarUtils.avatarToBitmap(mSession.senders().avatar(latestMessageId));
            }
            if (participantAvatar == null) {
                String initials = KulloUtils.generateInitialsForAddressAndName(participantName);
                participantAvatar = AvatarUtils.getSenderThumbnailFromInitials(context, initials);
            }
            out.participantsAvatar.put(address.toString(), participantAvatar);
        }

        // Title
        out.title = StringUtils.join(out.participantsTitle, ", ");

        // Counts
        out.countUnread = getConversationUnreadCount(conversationId);

        return out;
    }

    public String getConversationNameOrPlaceHolder(long conversationId) {
        AddressSet addresses = getParticipantAddresses(conversationId);

        List<String> participantTitles = new LinkedList<>();

        for (Address address : addresses.sorted()) {
            //check if conversation has this sender
            String participantName = getGlobalParticipantName(address);

            if (participantName != null && !participantName.isEmpty()) {
                participantTitles.add(participantName);
            } else {
                participantTitles.add(address.toString());
            }
        }

        return StringUtils.join(participantTitles, ", ");
    }

    @SuppressWarnings("WeakerAccess")
    @MainThread
    public int getConversationUnreadCount(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.conversations().unreadMessages(conversationId);
        }
    }

    @MainThread
    public void saveDraftForSending(long conversationId, @NonNull String message) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);

            // User is done writing. This trims the message before sending
            mSession.drafts().setText(conversationId, message.trim());
            mSession.drafts().prepareToSend(conversationId);
        }
    }

    @MainThread
    public void clearDraftForConversation(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.drafts().clear(conversationId);
        }
    }

    @NonNull
    @MainThread
    public DraftState getDraftState(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.drafts().state(conversationId);
        }
    }

    @NonNull
    @MainThread
    public String getDraftText(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.drafts().text(conversationId);
        }
    }

    @MainThread
    public void setDraftText(long conversationId, @NonNull String message) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.drafts().setText(conversationId, message);
        }
    }

    public long startConversationWithSingleRecipient(String recipientString) {
        Address recipient = AddressHelpers.create(recipientString);
        if (recipient == null) throw new RuntimeException("Invalid recipient address");
        AddressSet recipients = new AddressSet(Collections.singleton(recipient));
        return addNewConversationForKulloAddresses(recipients);
    }

    @MainThread
    public long addNewConversationForKulloAddresses(AddressSet participants) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.conversations().add(participants);
        }
    }

    public interface AddAttachmentToDraftCallback {
        void run(boolean success);
    }

    // ATTACHMENTS
    @NonNull
    @MainThread
    public AsyncTask addAttachmentToDraft(
            final long conversationId,
            @NonNull final String filePath,
            @NonNull final String mimeType,
            @Nullable final AddAttachmentToDraftCallback callback) {
        final AsyncTask task;

        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);

            task = mSession.draftAttachments().addAsync(conversationId, filePath, mimeType, new DraftAttachmentsAddListener() {
                @Override
                public void progressed(long convId, long attId, long bytesProcessed, long bytesTotal) {
                }

                @Override
                public void finished(long convId, long attId, @NonNull String path) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                        ((DraftAttachmentsAddListenerObserver) observer).finished(convId, attId, path);
                    }

                    if (callback != null) callback.run(true);
                }

                @Override
                public void error(long convId, @NonNull String path, @NonNull LocalError error) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                        ((DraftAttachmentsAddListenerObserver) observer).error(error);
                    }

                    if (callback != null) callback.run(false);
                }
            });
        }

        mTaskHolder.add(task);
        return task;
    }

    @MainThread
    public void removeDraftAttachment(long conversationId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.draftAttachments().remove(conversationId, attachmentId);
        }
    }

    @MainThread
    public ArrayList<Long> getAttachmentsForDraft(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.draftAttachments().allForDraft(conversationId);
        }
    }

    @MainThread
    public String getDraftAttachmentFilename(long conversationId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.draftAttachments().filename(conversationId, attachmentId);
        }
    }

    @MainThread
    public long getDraftAttachmentFilesize(long conversationId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.draftAttachments().size(conversationId, attachmentId);
        }
    }

    @MainThread
    public String getDraftAttachmentMimeType(long conversationId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.draftAttachments().mimeType(conversationId, attachmentId);
        }
    }

    @MainThread
    public void saveDraftAttachmentContent(long conversationId, long attachmentId, String path) {
        RuntimeAssertion.require(mSession != null);

        mTaskHolder.add(mSession.draftAttachments().saveToAsync(conversationId, attachmentId, path, new DraftAttachmentsSaveToListener() {
            @Override
            public void finished(long convId, long attId, @NonNull String path) {
                for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                    ((DraftAttachmentsSaveListenerObserver) observer).finished(convId, attId, path);
                }
            }

            @Override
            public void error(long convId, long attId, @NonNull String path, @NonNull LocalError error) {
                for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                    ((DraftAttachmentsSaveListenerObserver) observer).error(convId, attId, path, error.toString());
                }
            }
        }));
    }

    // PARTICIPANTS SENDERS

    @SuppressWarnings("WeakerAccess")
    @NonNull
    @MainThread
    public AddressSet getParticipantAddresses(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return new AddressSet(mSession.conversations().participants(conversationId));
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    @MainThread
    public String getGlobalParticipantName(Address address) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            RuntimeAssertion.require(address != null);

            long latestMessageId = mSession.messages().latestForSender(address);

            if (latestMessageId >= 0) {
                return mSession.senders().name(latestMessageId);
            } else {
                return null;
            }
        }
    }

    @NonNull
    @MainThread
    public Address getCurrentUserAddress() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.userSettings().address();
        }
    }

    @NonNull
    @MainThread
    public String getCurrentUserMasterKeyAsPem() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return MasterKeyHelpers.toPem(mSession.userSettings().masterKey());
        }
    }

    @NonNull
    @MainThread
    public String getCurrentUserName() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.userSettings().name();
        }
    }

    @MainThread
    public void setCurrentUserName(@NonNull String name) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.userSettings().setName(name);
        }
    }

    @NonNull
    @MainThread
    public String getCurrentUserOrganization() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.userSettings().organization();
        }
    }

    @MainThread
    public void setCurrentUserOrganization(@NonNull String organization) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.userSettings().setOrganization(organization);
        }
    }

    @NonNull
    @MainThread
    public String getCurrentUserFooter() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.userSettings().footer();
        }
    }

    @MainThread
    public void setCurrentUserFooter(@NonNull String footer) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.userSettings().setFooter(footer);
        }
    }

    @NonNull
    @MainThread
    public byte[] getCurrentUserAvatar() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.userSettings().avatar();
        }
    }

    @MainThread
    public void setCurrentUserAvatar(@NonNull byte[] avatar) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.userSettings().setAvatar(avatar);
        }
    }

    @MainThread
    public void setCurrentUserAvatarMimeType(@NonNull String avatarMimeType) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.userSettings().setAvatarMimeType(avatarMimeType);
        }
    }

    // MESSAGE

    @MainThread
    public void removeMessage(final long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.messages().remove(messageId);
        }
    }

    @NonNull
    @MainThread
    public List<Long> getAllMessageIdsSorted(long conversationId) {
        List<Long> out;

        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            out = mSession.messages().allForConversation(conversationId);
        }

        // List is sorted by id (equal to dateReceived); Reverse to get newest first
        Collections.reverse(out);
        return out;
    }

    @MainThread
    public int getMessageCount(long conversationId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.conversations().totalMessages(conversationId);
        }
    }

    @MainThread
    public long getMessageConversation(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messages().conversation(messageId);
        }
    }

    @NonNull
    @MainThread
    public String getMessageText(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messages().text(messageId);
        }
    }

    @NonNull
    @MainThread
    public String getMessageTextAsHtml(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messages().textAsHtml(messageId, true);
        }
    }

    @NonNull
    @MainThread
    public String getMessageFooter(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messages().footer(messageId);
        }
    }

    @MainThread
    public boolean getMessageUnread(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return !mSession.messages().isRead(messageId);
        }
    }

    @MainThread
    public boolean messageIncoming(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            Address me = mSession.userSettings().address();
            return !me.equals(mSession.senders().address(messageId));
        }
    }

    @MainThread
    public void setMessageRead(long messageId) {
        setMessagesRead(Collections.singleton(messageId));
    }

    @MainThread
    public void setMessagesRead(@NonNull final Set<Long> messageIds) {
        if (messageIds.isEmpty()) return;

        boolean anythingChanged = false;

        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            for (long messageId : messageIds) {
                anythingChanged |= mSession.messages().setRead(messageId, true);
            }
        }

        if (anythingChanged) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    sync();
                }
            });
        }
    }

    @NonNull
    @MainThread
    public org.joda.time.DateTime getMessageDateReceived(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return KulloUtils.convertToJodaTime(mSession.messages().dateReceived(messageId));
        }
    }

    @MainThread
    public boolean getMessageAttachmentsDownloaded(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messageAttachments().allAttachmentsDownloaded(messageId);
        }
    }

    @NonNull
    @MainThread
    public ArrayList<Long> getMessageAttachmentsIds(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messageAttachments().allForMessage(messageId);
        }
    }

    @MainThread
    public String getMessageAttachmentFilename(long messageId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messageAttachments().filename(messageId, attachmentId);
        }
    }

    @MainThread
    public long getMessageAttachmentFilesize(long messageId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messageAttachments().size(messageId, attachmentId);
        }
    }

    @MainThread
    public String getMessageAttachmentMimeType(long messageId, long attachmentId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.messageAttachments().mimeType(messageId, attachmentId);
        }
    }

    @MainThread
    public void downloadAttachments(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            mSession.syncer().requestDownloadingAttachmentsForMessage(messageId);
        }
    }

    public interface GetMessageAttachmentCallback {
        void run(byte[] data);
    }

    @MainThread
    public AsyncTask getMessageAttachment(long messageId, long attachmentId, final GetMessageAttachmentCallback callback) {
        final AsyncTask task;
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            task = mSession.messageAttachments().contentAsync(messageId, attachmentId, new MessageAttachmentsContentListener() {
                @Override
                public void finished(long msgId, long attId, byte[] content) {
                    int contentLegth = content.length;
                    callback.run(content);
                }
            });
        }
        mTaskHolder.add(task);
        return task;
    }

    @MainThread
    public void saveMessageAttachment(long messageId, long attachmentId, String path) {
        RuntimeAssertion.require(mSession != null);
        mTaskHolder.add(mSession.messageAttachments().saveToAsync(messageId, attachmentId, path, new MessageAttachmentsSaveToListener() {
            @Override
            public void finished(long msgId, long attId, @NonNull String path) {
                for (ListenerObserver observer : mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                    ((MessageAttachmentsSaveListenerObserver) observer).finished(msgId, attId, path);
                }
            }

            @Override
            public void error(long msgId, long attId, @NonNull String path, @NonNull LocalError error) {
                for (ListenerObserver observer : mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                    ((MessageAttachmentsSaveListenerObserver) observer).error(msgId, attId, path, error.toString());
                }
            }
        }));
    }

    @NonNull
    @MainThread
    public Address getSenderAddress(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.senders().address(messageId);
        }
    }

    @NonNull
    @MainThread
    public String getSenderName(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.senders().name(messageId);
        }
    }

    @NonNull
    @MainThread
    public String getSenderOrganization(long messageId) {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
            return mSession.senders().organization(messageId);
        }
    }

    /**
     * Gets a message's sender avatar if present, otherwise creates a placeholder showing the
     * sender's initials. If messageId does not exist (which happens during a delete call),
     * null is returned.
     */
    @Nullable
    @MainThread
    public Bitmap getSenderAvatar(Context context, long messageId) {
        RuntimeAssertion.require(mSession != null);

        final boolean messageExists = mSession.senders().address(messageId) != null;
        if (!messageExists) return null;

        final byte[] senderAvatar = mSession.senders().avatar(messageId);
        final String senderName = mSession.senders().name(messageId);

        Bitmap avatarBitmap = AvatarUtils.avatarToBitmap(senderAvatar);
        if (avatarBitmap != null) {
            return avatarBitmap;
        } else {
            String initials = KulloUtils.generateInitialsForAddressAndName(senderName);
            return AvatarUtils.getSenderThumbnailFromInitials(context, initials);
        }
    }

    // REGISTRATION
    @MainThread
    public void generateKeysAsync() {
        Client client = ClientConnector.get().getClient();

        mTaskHolder.add(client.generateKeysAsync(new ClientGenerateKeysListener() {
            @Override
            public void progress(byte progress) {
                for (ListenerObserver observer : mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                    ((ClientGenerateKeysListenerObserver) observer).progress(progress);
                }
            }

            @Override
            public void finished(@NonNull Registration registration) {
                mRegistration = registration;

                for (ListenerObserver observer : mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                    ((ClientGenerateKeysListenerObserver) observer).finished();
                }
            }
        }));
    }

    @MainThread
    public void registerAddressAsync(@NonNull final String addressString) {
        RuntimeAssertion.require(mRegistration != null);

        Address address = AddressHelpers.create(addressString);
        RuntimeAssertion.require(address != null);

        mTaskHolder.add(mRegistration.registerAccountAsync(
                address,
                KulloApplication.TERMS_URL,
                null,
                "",
                new RegistrationRegisterAccountListener() {
            @Override
            public void challengeNeeded(@NonNull Address address, @NonNull Challenge challenge) {
                for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    ((RegistrationRegisterAccountListenerObserver) observer).challengeNeeded(addressString, challenge);
                }
            }

            @Override
            public void addressNotAvailable(@NonNull Address address, @NonNull AddressNotAvailableReason reason) {
                for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    ((RegistrationRegisterAccountListenerObserver) observer).addressNotAvailable(addressString, reason);
                }
            }

            @Override
            public void finished(@NonNull Address address, @NonNull MasterKey masterKey) {
                for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    ((RegistrationRegisterAccountListenerObserver) observer).finished(address, masterKey);
                }
            }

            @Override
            public void error(@NonNull Address address, @NonNull NetworkError error) {
                for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    ((RegistrationRegisterAccountListenerObserver) observer).error(address, error);
                }
            }
        }));
    }

    // Push Notifications

    @MainThread
    // Tries to register a push token and does not wait for return value
    // Ensure that token is not set when calling this
    public void registerPushTokenAsync(@NonNull String registrationToken) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(mPushToken == null);

        mPushToken = new PushToken(PushTokenType.GCM, registrationToken,
            PushTokenEnvironment.ANDROID);

        mTaskHolder.add(mSession.registerPushToken(mPushToken));
    }

    public interface UnregisterPushTokenCallback {
        @MainThread
        void onDone(boolean success);
    }

    @MainThread
    public void tryUnregisterPushTokenAsync(final int timeoutMs, @NonNull final UnregisterPushTokenCallback callback) {
        if (mPushToken == null) {
            callback.onDone(true);
        } else {
            final AsyncTask unregisterPushTokenTask;
            synchronized (mSessionGuard) {
                RuntimeAssertion.require(mSession != null);
                unregisterPushTokenTask = mSession.unregisterPushToken(mPushToken);
            }

            new android.os.AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    unregisterPushTokenTask.waitForMs(timeoutMs);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (unregisterPushTokenTask.isDone()) {
                        mPushToken = null;
                        callback.onDone(true);
                    } else {
                        unregisterPushTokenTask.cancel(); // give up
                        mPushToken = null;
                        callback.onDone(false);
                    }
                }
            }.execute();
        }
    }

    public boolean hasPushToken() {
        synchronized (mSessionGuard) {
            RuntimeAssertion.require(mSession != null);
        }

        return mPushToken != null;
    }

    // Observers

    @AnyThread
    public void addListenerObserver(@NonNull Class type, @NonNull ListenerObserver observer) {
        mListenerObservers.get(type).add(observer);
    }

    @AnyThread
    public void removeListenerObserver(@NonNull Class type, @NonNull ListenerObserver observerToRemove) {
        final Iterator<ListenerObserver> itr = mListenerObservers.get(type).iterator();
        while (itr.hasNext()) {
            ListenerObserver o = itr.next();

            if (o.equals(observerToRemove)) {
                itr.remove();
                return;
            }
        }
        Log.w(TAG, "Listener observer to be removed not found for type: '" + type.toString() + "'");
    }

    @MainThread
    public void addEventObserver(@NonNull Class type, @NonNull EventObserver observer) {
        mEventObservers.get(type).add(observer);
    }

    @MainThread
    public void removeEventObserver(@NonNull Class type, @NonNull EventObserver observerToRemove) {
        final Iterator<EventObserver> itr = mEventObservers.get(type).iterator();
        while (itr.hasNext()) {
            EventObserver o = itr.next();

            if (o.equals(observerToRemove)) {
                itr.remove();
                return;
            }
        }
        Log.w(TAG, "Event observer to be removed not found for type: '" + type.toString() + "'");
    }
}
