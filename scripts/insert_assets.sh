#!/usr/bin/env bash


ASSETS_DIR="resources/assets"
BUILD_DIR="$1"


cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

if [ ! -d "$BUILD_DIR" ] ; then
    mkdir -p "$BUILD_DIR"
fi

cp -R "$ASSETS_DIR/" "$BUILD_DIR"
if [ $? -eq 0 ] ; then
    echo "Assets copied into $BUILD_DIR."
else
    echo "ERROR: Unable to copy assets from $ASSETS_DIR into $BUILD_DIR!"
fi
