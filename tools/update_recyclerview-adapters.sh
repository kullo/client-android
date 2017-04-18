#!/bin/bash
set -o errexit -o nounset -o pipefail
which shellcheck > /dev/null && shellcheck "$0"

PROJECT=".."
DST="$PROJECT/app/src/main/java/net/kullo/android/thirdparty"

SRC="$HOME/workspace/recyclerview-adapters/app/src/main/java/io/github/dialogsforandroid/recyclerviewadapters/library"

for file in "$SRC"/*; do
    basename=$(basename "$file");
    (
        echo "package net.kullo.android.thirdparty;"
        tail -n +2 "$file"
    ) > "$DST/$basename"
done
