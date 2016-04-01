#!/bin/bash
set -o errexit -o nounset -o pipefail
which shellcheck > /dev/null && shellcheck "$0"

TIME=$(date --rfc-3339=seconds)

FILENAME="screenshot $TIME.png"
REMOTE_PATH="/data/local/tmp/$FILENAME"

adb shell "/system/bin/screencap -p '$REMOTE_PATH'"
adb pull "$REMOTE_PATH" "$FILENAME"
