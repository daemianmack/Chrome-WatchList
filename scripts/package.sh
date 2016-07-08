#!/usr/bin/env bash


GREEN='\033[0;32m'
RESET='\033[0m'


cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
TARGET="$ROOT/target"
PROD_TARGET="$TARGET/prod"
OPTS_TARGET="$TARGET/options-prod"


VERSION_WITH_QUOTES=`cat project.clj | grep "defproject" | cut -d' ' -f3`
VERSION=`echo "${VERSION_WITH_QUOTES//\"}"`
TARGET_NAME="watchlist-$VERSION"

# Include stand-alone options app.
mv "$OPTS_TARGET"/* "$PROD_TARGET"

# Rename directory.
mv "$PROD_TARGET" "$TARGET/$TARGET_NAME"

scripts/insert_assets.sh "$TARGET/$TARGET_NAME"

echo
echo -e "${GREEN}$TARGET_NAME prepared for packing in $TARGET/$TARGET_NAME${RESET}"
echo
echo    "›  Use Chrome's Extensions -> 'Pack extension...' to pack it into a .crx file,"
echo    "›  or see https://developer.chrome.com/extensions/packaging#packaging."
