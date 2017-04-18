Kullo for Android
-----------------

## v44

 * Displays sender addresses in single message
 * Conversation info screen that lets you select participants
   to start a new conversation with
 * Shows download progress when downloading attachments
 * Shows image preview for image attachments
 * Suggests known Kullo addresses
 * Highlights Kullo addresses in message text

### Components

 * Update libkullo to v62
 * Update Botan to 2.1.0
 * Update SmartSqlite to v19
 * Update Android Support Library to 25.3.1

## v43 (2017-03-10)

 * Badges for unread messages count
 * Show account information in drawer
 * Improve link highlighting
 * Fix freezing app when adding large files to draft

### Components

 * Update libkullo to v61
 * Update SmartSqlite to v18
 * Update Android Support Library to 25.2.0

### Requirements

 * Google Play Services 9.8

## v42 (2017-02-11)

 * Improve link highlighting
 * Show attachments as grid
 * Fix crash bug when sharing images with Kullo
 * Fix crash bug when opening APK files on Android lower than 7.0
 * Add native code optimized for 64 bit Arm processors

### Components

 * Update libkullo to v60
 * Update Botan to 2.0.1
 * Update SmartSqlite to v17

## v41 (2016-12-20)

 * Allow scaling down images before sending
 * Improved file handling in share receiver and compose screen
 * Allow adding multiple attachments at once in compose screen
 * Exclude Kullo from Android Auto Backup for Apps
 * Sync automatically when app is running in background and push notification is received
 * Fix permission issue when receiving certain kind of file shares in Android 6+

### Components

 * Update libkullo to v59
 * Update Botan to 1.11.34

## v40 (2016-11-25)

 * Improved share receiver
 * Show upload progress when sending a message
 * Clean cache dir on app start
 * Fix closing of compose view when receiving a message

### Components

 * Update libkullo to v57
 * Update SmartSqlite to v16
 * Update Botan to 1.11.33

## v39 (2016-10-28)

 * Add share receiver
 * Fix sharing of different attachments with the same filename
 * Add missing filename extensions when attaching media files

## v38 (2016-10-07)

 * Handle several permission issues when selecting an avatar
 * Scroll messages list to top when a new message was written or received
 * Don't show notification while app is in the foreground; sync instead
 * Add push notification debugging infos in about screen

### Components

 * Update libkullo to v56
 * Update SmartSqlite to v15
 * Update JsonCpp to 1.7.6
 * Update Boost to 1.62.0
 * Update Botan to 1.11.32
 * Update Crystax NDK to 10.3.2

## v37 (2016-09-22)

 * Implement deleting of conversations including all messages
 * Fix crash when adding some kind of attachments
 * Show new conversation button when inbox is empty

### Components

 * Update libkullo to v55
 * Update SmartSqlite to v14
 * Update Botan to 1.11.31

### Requirements

 * Google Play Services 9.6

## v36 (2016-08-08)

 * Fix network issue when sending message to multiple recipients
 * Improve compression performance for large attachments
 * Store temporary camera images in the cache directory

### Components

 * Update libkullo to v54
 * Update SmartSqlite to v13

## v35 (2016-08-03)

 * Push notification: don't jump to inbox when there is a GCM availability error message
 * Push notification: properly check GCM availability when app is started at a different activity
 * Use stream method to open and share attachments
 * Store all temporary files in the cache directory
 * Other minor stability improvements

## v34 (2016-07-22)

 * Fix a timeout issue when sending or receiving large attachments
 * Fix crash when leaving inbox in landscape mode
 * Fix bug in "Save to" action for attachments
 * Use app-specific external dir to store camera images instead of "amfb" on SD card
 * Other minor stability improvements and dependency updates

### Components

 * Update libkullo to v53
 * Update SmartSqlite to v12

## v33 (2016-06-20)

 * Fixing a compatibility issue with message encryption

### Components

 * Update libkullo to v51
 * Update Botan to 1.11.30
 * Update SmartSqlite to v11

## v32 (2016-06-05)

 * Synchronize sender information
 * Update libkullo v49

## v31 (2016-04-06)

 * Fix timeout bug (occurred when sending large attachments)
 * Minor UI updates: initials generation, sync animation
 * Update libkullo v46

## v30 (2016-04-01)

 * Allow sending attachments of 100 MiB per message
 * Sort messages by server time
 * Update libkullo v45

## v29 (2016-03-03)

 * Allow OPEN WITH, SAVE AS and SHARE for attachments
 * Clear notifications when app comes to foreground
 * Let user switch between multiple accounts
 * Allow landscape orientation in most screens
 * Update libkullo v44

## v28 (2016-02-12)

 * Automatically sync when app is started or last sync more than 5 minutes ago
 * Notifications: use defaults for sound, light and vibration
 * Notifications: don't show multiple notifications at the same time
 * Notifications: always translate notification text
 * UI: Update system task switcher style
 * Fix crash when attachment is added via file mode (e.g. from ES file browser)
 * Update libkullo v43

## v27 (2016-02-04)

 * Don't show push notifications for outgoing messages

## v26 (2016-02-03)

 * Push notifications
 * Autocomplete domain when entering a Kullo address
 * Vibrate when key generation is done
 * Update libkullo v42
 * Update Botan 1.11.28

## v25 (2016-01-26)

 * UI: Optimize login/registration layout
 * UI: Don't cancel key generation when app goes to sleep
 * UI: Show full date and time in single message view
 * Update libkullo v41
 * Update SmartSqlite v5
 * Update Botan 1.11.26

## v24 (2015-12-22)

 * Improve error handling
 * Stop running tasks on logout
 * Update libkullo v40

## v23 (2015-12-11)

 * Show sync progress bar for long syncs
 * Minor UI updates and translations
 * Update libkullo v39
 * Update SmartSqlite v4
 * Update Botan 1.11.25

## v22 (2015-11-30)

 * Feature: Implement deleting of messages
 * UI: New app icon
 * UI: Improve scroll performance in conversations list
 * UI: Add write button to single message view
 * UI: Fix initial scroll position in single message view
 * UI: Minor style updates
 * UI: Fix multiple sync buttons bug
 * Update libkullo v38

## v21 (2015-11-26)

 * Improve database speed
 * Update libkullo v36
 * Update SmartSqlite v3

## v20 (2015-11-20)

 * Split messages in overview list and detail view
 * Mark message as read when detail view is opened

## v19 (2015-11-17)

 * Fix bug when opening a draft attachment
 * Use Android's local date and time format
 * Add third-party licenses

## v18 (2015-11-14)

 * Add image cropper for choosing an avatar
 * Allow selecting multiple conversations
 * Mark conversations that contain unread messages

## v17 (2015-11-05)

 * Sync buttons and pull-to-refresh in messages list
 * Allow removing of empty conversations
 * Simplify starting a conversation
 * Add Registration
 * Add screen to show MasterKey

## v16 (2015-10-26)

 * Fix broken conversation title on Android 5
 * UI updates and add missing translations
 * Update libraries

## v15 (2015-10-21)

 * Send attachments
 * Move avatars to the left of conversations list
 * Allow user to clear an existing avatar
 * Avoid crash when trying to open a file and SD card is not available

## v14 (2015-10-09)

 * Minor UI fixes

## v13 (2015-10-03)

 * Avoid crash when Kullo was not running for a while and is restarted in Messages List (thanks Frank)
 * Show attachments filesize
 * Update title in conversations list "today" -> "yesterday" when user opens the app after midnight

## v12 (2015-10-01)

 * Avoid crash when Kullo was not running for a while and is restarted (thanks Frank)
 * Save avatar as base64. This update throws away user's local avatar
 * Minor UI adjustments

## v11 (2015-09-26)

 * Open attachmets
 * Fix sorting bug in conversations list
 * Show time for messages received today

## v10 (2015-09-22)

 * Make message input box multiline again (regression bug v8 -> v9)
 * Improve sorting performance in messages list and conversations list
 * Fix crash after pressing "+" button to start a conversation (thanks Jens)
 * Minor UI adjustments

## v9 (2015-09-21)

 * Show list of attachments (which cannot be opened yet)
 * Fix crash after sending message (thanks Jens)
 * Minor UI, performance and stability improvements

## v8 (2015-09-15)

 * Improve performance and stability on sorting
 * Fix crash on sender names of length 1
 * Clear draft after sending
 * Settings: Save sender information live
 * Login: Store input data when app is switched
 * Logout: Add confirmation dialog

## v7 (2015-09-11)

 * Add Feedback and About screen
 * Refresh screen during sync
 * Check login credentials before moving on to inbox
 * Fix behavior on logout

### 3rd party components

 * Update Botan 1.11.20

## v6 (2015-09-09)

 * Optimize message input field for long text
 * Minor UI improvements like icon changes
 * Favour crash over logout when main screen is started in an unexpected way. That way, user does not have to type in the MasterKey again
 * Save message draft more reliably (thanks Colin)
 * Remove locally stored message database on logout

## v5 (2015-09-04)

 * Translate parts of the app to German
 * Replace @ by # in all Kullo address inputs
 * Update menu icons
 * Login: Live validation of MasterKey blocks; make validation more strict
 * Login: Fix app signals

## v4 (2015-09-02)

 * Improve sync speed
 * Improve sync feedback to UI
 * Fix conversations sorting

## v3 (2015-08-28)

 * Login: screen scrollable
 * Login: Cursor springt automatisch ins nächste Feld
 * Login: Gültigkeit einzelner Blocks wird geprüft
 * Nachricht schreiben: Landscape möglich
 * Überarbeitung der Signalverarbeitung

## v2 (2015-08-25)

 * Initial alpha release
