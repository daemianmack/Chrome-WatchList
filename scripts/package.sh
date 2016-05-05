#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

ROOT=`pwd`
BUILDS="$ROOT/builds"
PROD_BUILD="$BUILDS/prod"

if [ ! -d "$PROD_BUILD" ] ; then
  echo "'$PROD_BUILD' does not exist; running 'lein prod' first"
  lein prod
fi

VERSION_WITH_QUOTES=`cat project.clj | grep "defproject" | cut -d' ' -f3`
VERSION=`echo "${VERSION_WITH_QUOTES//\"}"`
TARGET_NAME="watchlist-$VERSION"

scripts/insert_assets.sh "$PROD_BUILD"

mv "$PROD_BUILD" "$BUILDS/$TARGET_NAME"

echo "'$TARGET_NAME' prepared for packing"
echo "  use Chrome's Window -> Extensions -> 'Pack extension...' to pack it into a .crx file"
echo "  or => https://developer.chrome.com/extensions/packaging#packaging"
