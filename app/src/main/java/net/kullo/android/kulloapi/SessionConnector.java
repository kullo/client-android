/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
import net.kullo.android.observers.listenerobservers.ClientCreateSessionListenerObserver;
import net.kullo.android.observers.listenerobservers.ClientGenerateKeysListenerObserver;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsAddListenerObserver;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsSaveListenerObserver;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;
import net.kullo.android.observers.listenerobservers.RegistrationRegisterAccountListenerObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.LoginActivity;
import net.kullo.android.screens.LogoutActivity;
import net.kullo.android.screens.WelcomeActivity;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.javautils.StrictBase64;
import net.kullo.libkullo.api.Address;
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
import net.kullo.libkullo.api.Event;
import net.kullo.libkullo.api.EventType;
import net.kullo.libkullo.api.InternalEvent;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.MessageAttachmentsSaveToListener;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.Registration;
import net.kullo.libkullo.api.RegistrationRegisterAccountListener;
import net.kullo.libkullo.api.Session;
import net.kullo.libkullo.api.SessionListener;
import net.kullo.libkullo.api.SyncMode;
import net.kullo.libkullo.api.SyncProgress;
import net.kullo.libkullo.api.SyncerListener;
import net.kullo.libkullo.api.UserSettings;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SessionConnector {
    private static final String TAG = "SessionConnector";

    private static final SessionConnector SINGLETON = new SessionConnector();
    @NonNull public static SessionConnector get() {
        return SINGLETON;
    }

    private AsyncTasksHolder mTaskHolder = new AsyncTasksHolder();
    private Session mSession = null;
    private String mRegistrationToken = null;
    private Registration mRegistration;

    private Map<Class, LinkedList<ListenerObserver>> mListenerObservers;
    private Map<Class, LinkedList<EventObserver>> mEventObservers;

    private SessionConnector() {
        mListenerObservers = new HashMap<>(10);
        mListenerObservers.put(ClientGenerateKeysListenerObserver.class, new LinkedList<ListenerObserver>());
        mListenerObservers.put(ClientCreateSessionListenerObserver.class, new LinkedList<ListenerObserver>());
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
    }

    //LOGIN LOGOUT

    @Nullable
    public AsyncTask createActivityWithSession(Activity callingActivity) {
        if (sessionAvailable()) return null;

        Log.d(TAG, "No session available. Creating session if credential are stored ...");
        UserSettings us = SessionConnector.get().loadStoredUserSettings(callingActivity);
        if (us == null) {
            Log.d(TAG, "No credentials found. Moving to WelcomeActivity ...");
            callingActivity.startActivity(new Intent(callingActivity, WelcomeActivity.class));
            callingActivity.finish();
            return null;
        }

        return SessionConnector.get().createSession(callingActivity, us);
    }

    public void checkLoginAndCreateSession(final LoginActivity callingActivity, final Address address, final MasterKey masterKey) {
        RuntimeAssertion.require(address != null);
        RuntimeAssertion.require(masterKey != null);

        Client client = ClientConnector.get().getClient();

        mTaskHolder.add(client.checkCredentialsAsync(address, masterKey, new ClientCheckCredentialsListener() {
            @Override
            public void finished(Address address, MasterKey masterKey, boolean valid) {
                if (valid) {
                    UserSettings us = UserSettings.create(address, masterKey);
                    storeLoginCredentialsInSharedPreferences(callingActivity, us);
                    createSession(callingActivity, us);
                } else {
                    synchronized (mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                        for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                            ((ClientCreateSessionListenerObserver) observer).loginFailed();
                        }
                    }
                }
            }

            @Override
            public void error(Address address, NetworkError error) {
                synchronized (mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                        ((ClientCreateSessionListenerObserver) observer).networkError(error);
                    }
                }
            }
        }));
    }

    public AsyncTask createSession(final Activity callingActivity, UserSettings us) {
        RuntimeAssertion.require(us != null);

        String dbFilePath = KulloUtils.getDatabasePathBase(callingActivity.getApplication(), us.address());
        Log.d(TAG, "dbFilePath = " + dbFilePath);

        Client client = ClientConnector.get().getClient();

        // Store task in local member to ensure it is not destroyed. Pass task as return
        // value to caller to enable it to block using AsyncTask#waitUntilDone()
        AsyncTask task = client.createSessionAsync(us, dbFilePath, new SessionListener() {
            @Override
            public void internalEvent(final InternalEvent event) {
                callingActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSession != null) {
                            ArrayList<Event> events = mSession.notify(event);
                            for (Event e : events) {
                                if (e.getEvent() == EventType.MESSAGEADDED) {
                                    synchronized (mEventObservers.get(MessageAddedEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(MessageAddedEventObserver.class)) {
                                            ((MessageAddedEventObserver) o).messageAdded(e.getConversationId(), e.getMessageId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.MESSAGEREMOVED) {
                                    synchronized (mEventObservers.get(MessageRemovedEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(MessageRemovedEventObserver.class)) {
                                            ((MessageRemovedEventObserver) o).messageRemoved(e.getConversationId(), e.getMessageId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.MESSAGESTATECHANGED) {
                                    synchronized (mEventObservers.get(MessageStateEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(MessageStateEventObserver.class)) {
                                            ((MessageStateEventObserver) o).messageStateChanged(e.getConversationId(), e.getMessageId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.MESSAGEATTACHMENTSDOWNLOADEDCHANGED) {
                                    synchronized (mEventObservers.get(MessageAttachmentsDownloadedChangedEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(MessageAttachmentsDownloadedChangedEventObserver.class)) {
                                            ((MessageAttachmentsDownloadedChangedEventObserver) o).messageAttachmentsDownloadedChanged(e.getMessageId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONADDED) {
                                    synchronized (mEventObservers.get(ConversationsEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                            ((ConversationsEventObserver) o).conversationAdded(e.getConversationId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONREMOVED) {
                                    synchronized (mEventObservers.get(ConversationsEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                            ((ConversationsEventObserver) o).conversationRemoved(e.getConversationId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.CONVERSATIONCHANGED) {
                                    synchronized (mEventObservers.get(ConversationsEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(ConversationsEventObserver.class)) {
                                            ((ConversationsEventObserver) o).conversationChanged(e.getConversationId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.DRAFTSTATECHANGED) {
                                    synchronized (mEventObservers.get(DraftEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(DraftEventObserver.class)) {
                                            ((DraftEventObserver) o).draftStateChanged(e.getConversationId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.DRAFTTEXTCHANGED) {
                                    synchronized (mEventObservers.get(DraftEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(DraftEventObserver.class)) {
                                            ((DraftEventObserver) o).draftTextChanged(e.getConversationId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.DRAFTATTACHMENTADDED) {
                                    synchronized (mEventObservers.get(DraftAttachmentAddedEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(DraftAttachmentAddedEventObserver.class)) {
                                            ((DraftAttachmentAddedEventObserver) o).draftAttachmentAdded(e.getConversationId(), e.getAttachmentId());
                                        }
                                    }
                                } else if (e.getEvent() == EventType.DRAFTATTACHMENTREMOVED) {
                                    synchronized (mEventObservers.get(DraftAttachmentRemovedEventObserver.class)) {
                                        for (EventObserver o : mEventObservers.get(DraftAttachmentRemovedEventObserver.class)) {
                                            ((DraftAttachmentRemovedEventObserver) o).draftAttachmentRemoved(e.getConversationId(), e.getAttachmentId());
                                        }
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

            @Override
            public void finished(Session session) {
                Log.d(TAG, "createSession finished :)");
                mSession = session;
                mSession.syncer().setListener(new ConnectorSyncerListener());

                synchronized (mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                        ((ClientCreateSessionListenerObserver) observer).finished();
                    }
                }
            }

            @Override
            public void error(Address address, LocalError error) {
                Log.d(TAG, address.toString() + " " + error.toString());

                synchronized (mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(ClientCreateSessionListenerObserver.class)) {
                        ((ClientCreateSessionListenerObserver) observer).localError(error);
                    }
                }
            }
        });

        mTaskHolder.add(task);

        return task;
    }

    public void storeSessionUserSettingsInSharedPreferences(final Context context) {
        UserSettings us = mSession.userSettings();
        storeLoginCredentialsInSharedPreferences(context, us);
    }

    public void storeAddressAndMasterkeyInSharedPreferences(final Context context, String addressString, String masterKeyAsPem) {
        MasterKey masterKey = MasterKey.createFromPem(masterKeyAsPem);
        RuntimeAssertion.require(masterKey != null);
        Address address = Address.create(addressString);
        RuntimeAssertion.require(address != null);
        UserSettings us = UserSettings.create(address, masterKey);
        storeLoginCredentialsInSharedPreferences(context, us);
    }

    public void storeLoginCredentialsInSharedPreferences(final Context context, UserSettings us) {
        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(
				KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(KulloConstants.KULLO_ADDRESS, us.address().toString());

        ArrayList<String> blockList = us.masterKey().dataBlocks();
        editor.putString(KulloConstants.BLOCK_A, blockList.get(0));
        editor.putString(KulloConstants.BLOCK_B, blockList.get(1));
        editor.putString(KulloConstants.BLOCK_C, blockList.get(2));
        editor.putString(KulloConstants.BLOCK_D, blockList.get(3));
        editor.putString(KulloConstants.BLOCK_E, blockList.get(4));
        editor.putString(KulloConstants.BLOCK_F, blockList.get(5));
        editor.putString(KulloConstants.BLOCK_G, blockList.get(6));
        editor.putString(KulloConstants.BLOCK_H, blockList.get(7));
        editor.putString(KulloConstants.BLOCK_I, blockList.get(8));
        editor.putString(KulloConstants.BLOCK_J, blockList.get(9));
        editor.putString(KulloConstants.BLOCK_K, blockList.get(10));
        editor.putString(KulloConstants.BLOCK_L, blockList.get(11));
        editor.putString(KulloConstants.BLOCK_M, blockList.get(12));
        editor.putString(KulloConstants.BLOCK_N, blockList.get(13));
        editor.putString(KulloConstants.BLOCK_O, blockList.get(14));
        editor.putString(KulloConstants.BLOCK_P, blockList.get(15));

        final String KEY_NAME             = "user_name_" + us.address().toString();
        final String KEY_ORGANIZATION     = "user_organization_" + us.address().toString();
        final String KEY_FOOTER           = "user_footer_" + us.address().toString();
        final String KEY_AVATAR           = "user_avatar_" + us.address().toString();
        final String KEY_AVATAR_MIME_TYPE = "user_avatar_mime_type_" + us.address().toString();

        editor.putString(KEY_NAME, us.name());
        editor.putString(KEY_ORGANIZATION, us.organization());
        editor.putString(KEY_FOOTER, us.footer());
        editor.putString(KEY_AVATAR, StrictBase64.encode(us.avatar()));
        editor.putString(KEY_AVATAR_MIME_TYPE, us.avatarMimeType());

        editor.commit();
    }

    @Nullable
    public UserSettings loadStoredUserSettings(Context context) {
        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(
				KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE);
        String addressString = sharedPref.getString(KulloConstants.KULLO_ADDRESS, "");

        ArrayList<String> blockList = new ArrayList<>(Arrays.asList(
                sharedPref.getString(KulloConstants.BLOCK_A, ""),
                sharedPref.getString(KulloConstants.BLOCK_B, ""),
                sharedPref.getString(KulloConstants.BLOCK_C, ""),
                sharedPref.getString(KulloConstants.BLOCK_D, ""),
                sharedPref.getString(KulloConstants.BLOCK_E, ""),
                sharedPref.getString(KulloConstants.BLOCK_F, ""),
                sharedPref.getString(KulloConstants.BLOCK_G, ""),
                sharedPref.getString(KulloConstants.BLOCK_H, ""),
                sharedPref.getString(KulloConstants.BLOCK_I, ""),
                sharedPref.getString(KulloConstants.BLOCK_J, ""),
                sharedPref.getString(KulloConstants.BLOCK_K, ""),
                sharedPref.getString(KulloConstants.BLOCK_L, ""),
                sharedPref.getString(KulloConstants.BLOCK_M, ""),
                sharedPref.getString(KulloConstants.BLOCK_N, ""),
                sharedPref.getString(KulloConstants.BLOCK_O, ""),
                sharedPref.getString(KulloConstants.BLOCK_P, "")
        ));

        // Check validity of loaded login data
        Address address = Address.create(addressString);
        MasterKey masterKey = MasterKey.createFromDataBlocks(blockList);
        if (address == null || masterKey == null) {
            return null;
        }

        final String KEY_NAME             = "user_name_" + address.toString();
        final String KEY_ORGANIZATION     = "user_organization_" + address.toString();
        final String KEY_FOOTER           = "user_footer_" + address.toString();
        final String KEY_AVATAR           = "user_avatar_" + address.toString();
        final String KEY_AVATAR_MIME_TYPE = "user_avatar_mime_type_" + address.toString();

        UserSettings out = UserSettings.create(address, masterKey);
        out.setName(sharedPref.getString(KEY_NAME, ""));
        out.setOrganization(sharedPref.getString(KEY_ORGANIZATION, ""));
        out.setFooter(sharedPref.getString(KEY_FOOTER, ""));
        try {
            out.setAvatar(StrictBase64.decode(sharedPref.getString(KEY_AVATAR, "")));
        } catch (StrictBase64.DecodingException e) {
            out.setAvatar(new byte[0]);
        }
        out.setAvatarMimeType(sharedPref.getString(KEY_AVATAR_MIME_TYPE, ""));

        // Avoid avatar-avatarMimeType inconsistency
        if (out.avatarMimeType().isEmpty() || out.avatar().length == 0) {
            out.setAvatar(new byte[0]);
            out.setAvatarMimeType("");
        }

        return out;
    }

    public boolean userSettingsAreValidForSync() {
        return !mSession.userSettings().name().isEmpty();
    }

    /*
     * Callback runs on UI thread
     */
    public void logout(final Activity callingActivity, final Runnable callback) {
        Log.d(TAG, "Logging out user ...");

        if (mSession != null) {
            new android.os.AsyncTask<Void, Void, Void>() {
                private Address address;

                @Override
                protected void onPreExecute() {
                    // Access UserSetting here on UI thread
                    address = mSession.userSettings().address();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    mSession.syncer().cancel();
                    mTaskHolder.cancelAll();
                    mSession.syncer().waitUntilDone();
                    mTaskHolder.waitUntilAllDone();

                    // Since all workers are done, this should be the last reference
                    // of the session.
                    mSession = null;

                    // SystemClock.sleep(2000);

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
                            KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE);
                    sharedPref.edit().clear().apply();

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

    public boolean sessionAvailable() {
        return mSession != null;
    }


    // SYNC
    public boolean isSyncing() {
        return mSession.syncer().isSyncing();
    }

    public void syncKullo() {
        RuntimeAssertion.require(mSession != null);
        mSession.syncer().requestSync(SyncMode.WITHOUTATTACHMENTS);
    }

    public void sendMessages() {
        RuntimeAssertion.require(mSession != null);
        mSession.syncer().requestSync(SyncMode.SENDONLY);
    }

    private class ConnectorSyncerListener extends SyncerListener {
        @Override
        public void started() {
            synchronized (mListenerObservers.get(SyncerListenerObserver.class)) {
                for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                    ((SyncerListenerObserver) observer).started();
                }
            }
        }

        @Override
        public void draftAttachmentsTooBig(long convId) {
            synchronized (mListenerObservers.get(SyncerListenerObserver.class)) {
                for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                    ((SyncerListenerObserver) observer).draftAttachmentsTooBig(convId);
                }
            }
        }

        @Override
        public void progressed(SyncProgress progress) {
            synchronized (mListenerObservers.get(SyncerListenerObserver.class)) {
                for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                    ((SyncerListenerObserver) observer).progressed(progress);
                }
            }
        }

        @Override
        public void finished() {
            synchronized (mListenerObservers.get(SyncerListenerObserver.class)) {
                for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                    ((SyncerListenerObserver) observer).finished();
                }
            }
        }

        @Override
        public void error(NetworkError error) {
            synchronized (mListenerObservers.get(SyncerListenerObserver.class)) {
                for (ListenerObserver observer : mListenerObservers.get(SyncerListenerObserver.class)) {
                    ((SyncerListenerObserver) observer).error(error);
                }
            }
        }
    }

    // CONVERSATIONS

    @NonNull
    public List<Long> getAllConversationIdsSorted() {
        RuntimeAssertion.require(mSession != null);

        ArrayList<Long> allConversationIds = mSession.conversations().all();
        KulloComparator comparator = new ConversationsComparatorDsc(mSession);
        KulloSort.sort(allConversationIds, comparator);
        Log.d(TAG, "Done sorting conversations. " + comparator.getStats());

        return allConversationIds;
    }

    @NonNull
    public org.joda.time.DateTime getLatestMessageTimestamp(long conversationId) {
        RuntimeAssertion.require(mSession != null);

        DateTime latestMessageTimestamp = mSession.conversations().latestMessageTimestamp(conversationId);
        return KulloUtils.convertToJodaTime(latestMessageTimestamp);
    }

    @NonNull
    public org.joda.time.DateTime emptyConversationTimestamp() {
        return KulloUtils.convertToJodaTime(Conversations.emptyConversationTimestamp());
    }

    public void removeConversation(long conversationId) {
        RuntimeAssertion.require(mSession != null);
        mSession.conversations().remove(conversationId);
    }

    // Get all data at once to avoid unnecessary JNI calls when scolling the list
    public ConversationData getConversationData(Context context, long conversationId) {
        ConversationData out = new ConversationData();
        out.mParticipants = getParticipantAddresses(conversationId);

        for (Address address : out.mParticipants.sorted()) {
            long latestMessageId = mSession.messages().latestForSender(address);
            String participantName = latestMessageId != -1 ? mSession.senders().name(latestMessageId) : "";

            // Title
            String participantTitle = !participantName.isEmpty() ? participantName : address.toString();
            out.mParticipantsTitles.add(participantTitle);

            // Avatar
            Bitmap participantAvatar = null;
            if (latestMessageId != -1) {
                participantAvatar = AvatarUtils.avatarToBitmap(mSession.senders().avatar(latestMessageId));
            }
            if (participantAvatar == null) {
                String initials = KulloUtils.generateInitialsForAddressAndName(address, participantName);
                participantAvatar = AvatarUtils.getSenderThumbnailFromInitials(context, initials);
            }
            out.mParticipantsAvatars.add(participantAvatar);
        }

        // Title
        out.mTitle = StringUtils.join(out.mParticipantsTitles, ", ");

        // Counts
        out.mCountUnread = getConversationUnreadCount(conversationId);

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

    public int getConversationUnreadCount(long conversationId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.conversations().unreadMessages(conversationId);
    }

    public void saveDraftForConversation(long conversationId, String message, boolean prepareToSend) {
        RuntimeAssertion.require(mSession != null);

        if (prepareToSend) {
            // User is done writing. This trims the message before sending
            mSession.drafts().setText(conversationId, message.trim());
            mSession.drafts().prepareToSend(conversationId);
        } else {
            mSession.drafts().setText(conversationId, message);
        }
    }

    public void clearDraftForConversation(long conversationId) {
        RuntimeAssertion.require(mSession != null);

        mSession.drafts().clear(conversationId);
    }

    public String getDraftText(long conversationId) {
        RuntimeAssertion.require(mSession != null);

        return mSession.drafts().text(conversationId);
    }

    public long startConversationWithSingleRecipient(String recipientString) {
        Address recipient = Address.create(recipientString);
        if (recipient == null) throw new RuntimeException("Invalid recipient address");
        AddressSet recipients = new AddressSet(Collections.singleton(recipient));
        return addNewConversationForKulloAddresses(recipients);
    }

    public long addNewConversationForKulloAddresses(AddressSet participants) {
        RuntimeAssertion.require(mSession != null);

        return mSession.conversations().add(participants);
    }

    // ATTACHMENTS

    public AsyncTask addAttachmentToDraft(long conversationId, String filePath, String mimeType) {
        RuntimeAssertion.require(mSession != null);
        return mSession.draftAttachments().addAsync(conversationId, filePath, mimeType, new DraftAttachmentsAddListener() {
            @Override
            public void finished(long convId, long attId, String path) {
                synchronized (mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                        ((DraftAttachmentsAddListenerObserver) observer).finished(convId, attId, path);
                    }
                }
            }

            @Override
            public void error(long convId, String path, LocalError error) {
                synchronized (mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsAddListenerObserver.class)) {
                        ((DraftAttachmentsAddListenerObserver) observer).error(error.toString());
                    }
                }
            }
        });
    }

    public void removeDraftAttachment(long conversationId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        mSession.draftAttachments().remove(conversationId, attachmentId);
    }

    public ArrayList<Long> getAttachmentsForDraft(long conversationId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.draftAttachments().allForDraft(conversationId);
    }

    public String getDraftAttachmentFilename(long conversationId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.draftAttachments().filename(conversationId, attachmentId);
    }

    public long getDraftAttachmentFilesize(long conversationId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.draftAttachments().size(conversationId, attachmentId);
    }

    public String getDraftAttachmentMimeType(long conversationId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.draftAttachments().mimeType(conversationId, attachmentId);
    }

    public void saveDraftAttachmentContent(long conversationId, long attachmentId, String path) {
        RuntimeAssertion.require(mSession != null);

        mTaskHolder.add(mSession.draftAttachments().saveToAsync(conversationId, attachmentId, path, new DraftAttachmentsSaveToListener() {
            @Override
            public void finished(long convId, long attId, String path) {
                synchronized (mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                        ((DraftAttachmentsSaveListenerObserver) observer).finished(convId, attId, path);
                    }
                }
            }

            @Override
            public void error(long convId, long attId, String path, LocalError error) {
                synchronized (mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(DraftAttachmentsSaveListenerObserver.class)) {
                        ((DraftAttachmentsSaveListenerObserver) observer).error(convId, attId, path, error.toString());
                    }
                }
            }
        }));
    }

    // PARTICIPANTS SENDERS

    @NonNull
    public AddressSet getParticipantAddresses(long conversationId) {
        RuntimeAssertion.require(mSession != null);

        return new AddressSet(mSession.conversations().participants(conversationId));
    }

    @Nullable
    public String getGlobalParticipantName(Address address) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(address != null);

        long latestMessageId = mSession.messages().latestForSender(address);

        if (latestMessageId >= 0) {
            return mSession.senders().name(latestMessageId);
        } else {
            return null;
        }
    }

    //CLIENT CURRENT USER

    public String getClientName() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().name();
    }

    public String getClientOrganization() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().organization();
    }

    public String getClientFooter() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().footer();
    }

    public void setClientName(String name) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(name != null);
        mSession.userSettings().setName(name);
    }

    public void setClientOrganization(String organization) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(organization != null);
        mSession.userSettings().setOrganization(organization);
    }

    public void setClientFooter(String footer) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(footer != null);
        mSession.userSettings().setFooter(footer);
    }

    public void setClientAvatar(byte[] avatar) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(avatar != null);
        mSession.userSettings().setAvatar(avatar);
    }

    public void setClientAvatarMimeType(String avatarMimeType) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(avatarMimeType != null);
        mSession.userSettings().setAvatarMimeType(avatarMimeType);
    }

    public String getClientAddressAsString() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().address().toString();
    }

    public byte[] getClientAvatar() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().avatar();
    }

    public String getMasterKeyAsPem() {
        RuntimeAssertion.require(mSession != null);
        return mSession.userSettings().masterKey().pem();
    }

    // MESSAGE

    public void removeMessage(final long messageId) {
        RuntimeAssertion.require(mSession != null);
        mSession.messages().remove(messageId);
    }

    @NonNull
    public ArrayList<Long> getAllMessageIdsSorted(long conversationId) {
        RuntimeAssertion.require(mSession != null);

        // presort IDs list (by ID, descending)
        ArrayList<Long> list = mSession.messages().allForConversation(conversationId); // unsorted
        Collections.reverse(list);

        KulloComparator countingComp = new MessagesComparatorDsc(mSession);
        KulloSort.insertionsort(list, countingComp);
        Log.d(TAG, "Done sorting messages. " + countingComp.getStats());

        return list;
    }

    public int getMessageCount(long conversationId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messages().allForConversation(conversationId).size();
    }

    public long getMessageConversation(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messages().conversation(messageId);
    }

    @NonNull
    public String getMessageText(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messages().text(messageId);
    }

    @NonNull
    public String getMessageFooter(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messages().footer(messageId);
    }

    public boolean getMessageUnread(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return !mSession.messages().isRead(messageId);
    }

    public void setMessageRead(long messageId) {
        RuntimeAssertion.require(mSession != null);
        mSession.messages().setRead(messageId, true);
    }

    @NonNull
    public org.joda.time.DateTime getMessageDateReceived(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return KulloUtils.convertToJodaTime(mSession.messages().dateReceived(messageId));
    }

    public boolean getMessageAttachmentsDownloaded(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messageAttachments().allAttachmentsDownloaded(messageId);
    }

    public ArrayList<Long> getMessageAttachmentsIds(long messageId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messageAttachments().allForMessage(messageId);
    }

    public String getMessageAttachmentFilename(long messageId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messageAttachments().filename(messageId, attachmentId);
    }

    public long getMessageAttachmentFilesize(long messageId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messageAttachments().size(messageId, attachmentId);
    }

    public String getMessageAttachmentMimeType(long messageId, long attachmentId) {
        RuntimeAssertion.require(mSession != null);
        return mSession.messageAttachments().mimeType(messageId, attachmentId);
    }

    public void downloadAttachments(long messageId) {
        RuntimeAssertion.require(mSession != null);
        mSession.syncer().requestDownloadingAttachmentsForMessage(messageId);
    }

    public void saveMessageAttachment(long messageId, long attachmentId, String path) {
        RuntimeAssertion.require(mSession != null);
        mTaskHolder.add(mSession.messageAttachments().saveToAsync(messageId, attachmentId, path, new MessageAttachmentsSaveToListener() {
            @Override
            public void finished(long msgId, long attId, String path) {
                synchronized (mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                        ((MessageAttachmentsSaveListenerObserver) observer).finished(msgId, attId, path);
                    }
                }
            }

            @Override
            public void error(long msgId, long attId, String path, LocalError error) {
                synchronized (mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(MessageAttachmentsSaveListenerObserver.class)) {
                        ((MessageAttachmentsSaveListenerObserver) observer).error(msgId, attId, path, error.toString());
                    }
                }
            }
        }));
    }

    @NonNull
    public String getSenderName(long messageId) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(messageId > 0);

        return mSession.senders().name(messageId);
    }

    @NonNull
    public String getSenderOrganization(long messageId) {
        RuntimeAssertion.require(mSession != null);
        RuntimeAssertion.require(messageId > 0);

        return mSession.senders().organization(messageId);
    }

    /**
     * Gets a message's sender avatar if present, otherwise creates a placeholder showing the
     * sender's initials. If messageId does not exist (which happens during a delete call),
     * null is returned.
     */
    @Nullable
    public Bitmap getSenderAvatar(Context context, long messageId) {
        RuntimeAssertion.require(mSession != null);

        Bitmap avatarBitmap = AvatarUtils.avatarToBitmap(mSession.senders().avatar(messageId));
        if (avatarBitmap != null) {
            return avatarBitmap;
        }

        Address senderAddress = mSession.senders().address(messageId);
        if (senderAddress != null) {
            String senderName = mSession.senders().name(messageId);
            String initials = KulloUtils.generateInitialsForAddressAndName(senderAddress, senderName);
            return AvatarUtils.getSenderThumbnailFromInitials(context, initials);
        }

        // message does not exist
        return null;
    }

    // REGISTRATION
    public void generateKeysAsync() {
        Client client = ClientConnector.get().getClient();

        mTaskHolder.add(client.generateKeysAsync(new ClientGenerateKeysListener() {
            @Override
            public void progress(byte progress) {
                synchronized (mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                        ((ClientGenerateKeysListenerObserver) observer).progress(progress);
                    }
                }
            }

            @Override
            public void finished(Registration registration) {
                mRegistration = registration;
                synchronized (mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(ClientGenerateKeysListenerObserver.class)) {
                        ((ClientGenerateKeysListenerObserver) observer).finished();
                    }
                }
            }
        }));
    }

    public void registerAddressAsync(@NonNull final String addressString) {
        RuntimeAssertion.require(mRegistration != null);

        Address address = Address.create(addressString);
        RuntimeAssertion.require(address != null);

        mTaskHolder.add(mRegistration.registerAccountAsync(address, null, "", new RegistrationRegisterAccountListener() {
            @Override
            public void challengeNeeded(Address address, Challenge challenge) {
                synchronized (mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                        ((RegistrationRegisterAccountListenerObserver) observer).challengeNeeded(addressString, challenge);
                    }
                }
            }

            @Override
            public void addressNotAvailable(Address address, AddressNotAvailableReason reason) {
                synchronized (mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                        ((RegistrationRegisterAccountListenerObserver) observer).addressNotAvailable(addressString, reason);
                    }
                }
            }

            @Override
            public void finished(Address address, MasterKey masterKey) {
                synchronized (mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                        ((RegistrationRegisterAccountListenerObserver) observer).finished(addressString, masterKey.pem());
                    }
                }
            }

            @Override
            public void error(Address address, NetworkError error) {
                synchronized (mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                    for (ListenerObserver observer : mListenerObservers.get(RegistrationRegisterAccountListenerObserver.class)) {
                        ((RegistrationRegisterAccountListenerObserver) observer).error(address, error);
                    }
                }
            }
        }));
    }

    // Push Notifications

    public void registerPushToken(String registrationToken) {
        RuntimeAssertion.require(mSession != null);

        // Token changed? unregister old one
        if (mRegistrationToken != null) {
            unregisterPushToken();
        }

        // store token within session instance
        mRegistrationToken = registrationToken;
        AsyncTask registerTask = mSession.registerPushToken(registrationToken);
        if (registerTask != null) {
            registerTask.waitUntilDone();
        }
    }

    public void unregisterPushToken() {
        RuntimeAssertion.require(mSession != null);

        if (mRegistrationToken != null) {
            AsyncTask unregisterTask = mSession.unregisterPushToken(mRegistrationToken);
            if (unregisterTask != null) {
                unregisterTask.waitUntilDone();
            }
            mRegistrationToken = null;
        }
    }

    public boolean hasPushToken() {
        RuntimeAssertion.require(mSession != null);

        return mRegistrationToken != null;
    }

    // Observers

    public void addListenerObserver(Class type, ListenerObserver observer) {
        synchronized (mListenerObservers.get(type)) {
            mListenerObservers.get(type).add(observer);
        }
    }

    public void removeListenerObserver(Class type, ListenerObserver observerToRemove) {
        synchronized (mListenerObservers.get(type)) {
            Iterator<ListenerObserver> itr = mListenerObservers.get(type).iterator();
            while (itr.hasNext()) {
                ListenerObserver o = itr.next();

                if (o.equals(observerToRemove)) {
                    itr.remove();
                    return;
                }
            }
        }
        Log.w(TAG, "Listener observer to be removed not found for type: '" + type.toString() + "'");
    }

    public void addEventObserver(Class type, EventObserver observer) {
        synchronized (mEventObservers.get(type)) {
            mEventObservers.get(type).add(observer);
        }
    }

    public void removeEventObserver(Class type, EventObserver observerToRemove) {
        synchronized (mEventObservers.get(type)) {
            Iterator<EventObserver> itr = mEventObservers.get(type).iterator();
            while (itr.hasNext()) {
                EventObserver o = itr.next();

                if (o.equals(observerToRemove)) {
                    itr.remove();
                    return;
                }
            }
        }
        Log.w(TAG, "Event observer to be removed not found for type: '" + type.toString() + "'");
    }

    @NonNull
    public Session getSession() {
        RuntimeAssertion.require(mSession != null);
        return mSession;
    }
}
