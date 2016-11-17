#!/bin/bash
set -o errexit -o nounset -o pipefail
which shellcheck > /dev/null && shellcheck "$0"

function usage() {
    echo "$0 FILEPATH SIZE OUT_FILENAME"
    echo ""
    echo "FILEPATH      The path to the svg"
    echo "SIZE          18, 24, 36, 48"
    echo "OUT_FILENAME  e.g. ic_expand_more_gray"
}

FIRST=${1:-}
SIZE=${2:-}
THIRD=${3:-}

if [ "$FIRST" == "" ]; then
    usage
    exit 1
elif [ "$FIRST" == "-h" ] || [ "$FIRST" == "--help" ]; then
    usage
    exit 0
fi

if [ "$SIZE" == "" ]; then
    usage
    exit 1
fi

if [ "$THIRD" == "" ]; then
    usage
    exit 1
fi

echo "Done parsing input."

SVG="$FIRST"
OUT_FILENAME="${THIRD}_${SIZE}dp.png"

RESOLUTIONS=(mdpi hdpi xhdpi xxhdpi xxxhdpi)

declare -A FACTORS=(
    ["ldpi"]=0.75
    ["mdpi"]=1.0
    ["hdpi"]=1.5
    ["xhdpi"]=2.0
    ["xxhdpi"]=3.0
    ["xxxhdpi"]=4.0
)

for RES in "${RESOLUTIONS[@]}"; do
    echo "$RES"
    PNG_SIZE=$(echo "(${FACTORS[$RES]}*$SIZE)/1" | bc)
    echo "$PNG_SIZE"
    TARGET_DIR="tmp/drawable-$RES"
    mkdir -p "$TARGET_DIR"
    inkscape --without-gui --file="$SVG" \
        --export-png="$TARGET_DIR/$OUT_FILENAME" \
        --export-width="$PNG_SIZE"
    optipng "$TARGET_DIR/$OUT_FILENAME"
done

