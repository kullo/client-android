This code is part of the Kullo client. An overview of all related
repositories can be found here:
[https://www.kullo.net/open/](https://www.kullo.net/open/)
***********

# Kullo for Android

## Debugging

Check files from `adb shell`:

    run-as net.kullo.android ls -l /data/data/net.kullo.android
    run-as net.kullo.android ls -l /data/data/net.kullo.android/shared_prefs

Read file contents from `adb shell`:

    run-as net.kullo.android cat /data/data/net.kullo.android/shared_prefs/net.kullo.android.SETTINGS.xml
    run-as net.kullo.android cat /data/data/net.kullo.android/shared_prefs/net.kullo.android.ACCOUNT_PREFS.xml
    run-as net.kullo.android cat /data/data/net.kullo.android/shared_prefs/net.kullo.android.USER_SETTINGS_PREFS.xml

### Local databases

List database files

    run-as net.kullo.android ls -l /data/data/net.kullo.android/files/

Remove a file db file

    run-as net.kullo.android rm /data/data/net.kullo.android/files/<FILENAME>

Pull files folder from device (Caution! Makes data world-writable)

    adb shell "run-as net.kullo.android chmod -R 777 /data/data/net.kullo.android/files" && \
        adb shell "mkdir -p /sdcard/tempDB" && \
        adb shell "cp -r /data/data/net.kullo.android/files/ /sdcard/tempDB/." && \
        adb pull sdcard/tempDB/ . && \
        adb shell "rm -r /sdcard/tempDB/*"

### Starting activities

Since there is no single entry point for Android apps, we need to make
sure Kullo starts properly with every activity. Some activities cannot be
run by right-klicking it from Android Studio since they need additional
Intent data, e.g. the messages list needs a conversation id > 0.

Starting activities from adb shell:

    am start  -n "net.kullo.android/net.kullo.android.screens.MessagesListActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER --el conversation_id 2

 * Integer extra: --ei
 * Long extra: --el
 * String extra: --es or --e

