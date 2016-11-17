#!/bin/bash
set -o errexit -o nounset -o pipefail
which shellcheck > /dev/null && shellcheck "$0"

function usage() {
    echo "$0 ICON_NAME COLOR SIZE"
    echo ""
    echo "ICON_NAME    name of the icon (e.g. ic_expand_more)"
    echo "COLOR        \"black\" or \"white\""
    echo "SIZE         18, 24, 36, 48"
    echo ""
    echo "Set env variable MI_ROOT to material icons repository checkout."
}

function filter_icon_orientation_not_rtl() {
    grep -v '\-ldrtl\-'
}

FIRST=${1:-}

if [ "$FIRST" = "" ]; then
    usage
    exit 1
elif [ "$FIRST" = "-h" ] || [ "$FIRST" = "--help" ]; then
    usage
    exit 0
fi

NAME="$FIRST"
FORMAT="png"
OS_PATTERN="drawable-"
COLOR=${2:="black"}
SRC_ROOT=${MI_ROOT:="$HOME/nobackup/material-design-icons/"}

# Are menu icons really 48dp?
# http://stackoverflow.com/a/21311302
SIZE=${3:="48"}

RESOLUTIONS=(mdpi hdpi xhdpi xxhdpi xxxhdpi)

if [ ! -d "$SRC_ROOT" ]; then
    echo "Source folder does not exist: $SRC_ROOT"
    echo "Set envorinment variable MI_ROOT."
    exit 1
fi

WD="$PWD" # fix inital working directory
while [ ! -f "$PWD/android.iml" ] && [ "$PWD" != "/" ]; do
    cd ..
done
# now $PWD is either "/" or the correct dir. Try it.
ANDROID_DIR="$PWD"
cd "$WD"

DST_ROOT="$ANDROID_DIR/app/src/main/res"
if [ ! -d "$DST_ROOT" ]; then
    echo "Destination folder does not exist: $DST_ROOT"
    echo "Make sure to \`cd\` into your Android project."
    exit 1
fi

if ! find "$SRC_ROOT" -name "${NAME}_${COLOR}_${SIZE}dp.${FORMAT}" | grep "$NAME" > /dev/null; then
    echo "Icon with name '$NAME' (e.g. ic_exit_to_app) was not found in $SRC_ROOT."
    exit 1
fi

for RES in "${RESOLUTIONS[@]}"
do
    FILE=$(find "$SRC_ROOT" -name "${NAME}_${COLOR}_${SIZE}dp.${FORMAT}" \
            | grep "${OS_PATTERN}" \
            | filter_icon_orientation_not_rtl \
            | grep "\-$RES")
    DST_FILENAME="${NAME}_${COLOR}_${SIZE}dp.$FORMAT"

    echo "$FILE" "➜ " "$DST_FILENAME"

    cp "$FILE" "$DST_ROOT/drawable-$RES/$DST_FILENAME"
done

