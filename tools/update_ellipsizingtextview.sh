#!/bin/bash
set -o errexit -o nounset -o pipefail
which shellcheck > /dev/null && shellcheck "$0"

PROJECT=".."
DST="$PROJECT/app/src/main/java/net/kullo/android/thirdparty"

SRC="$HOME/workspace/EllipsizingTextView"

for file in "$SRC"/*.java; do
    basename=$(basename "$file");
    (
        echo "package net.kullo.android.thirdparty;"
        tail -n +2 "$file"
    ) > "$DST/$basename"
done
