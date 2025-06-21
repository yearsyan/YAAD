#!/bin/bash

# Exit on error
set -e

export OPENSSL_VERSION="3.0.15-4"
export BOOST_VERSION="1.85.0"
# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT=$SCRIPT_DIR
echo "PROJECT_ROOT: $PROJECT_ROOT"

# Try to find Android NDK from various environment variables
if [ -z "$ANDROID_NDK_ROOT" ]; then
    if [ -n "$ANDROID_NDK" ]; then
        ANDROID_NDK_ROOT="$ANDROID_NDK"
    elif [ -n "$ANDROID_NDK_HOME" ]; then
        ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
    else
        echo "Error: ANDROID_NDK_ROOT, ANDROID_NDK, or ANDROID_NDK_HOME must be set"
        exit 1
    fi
fi

source "$SCRIPT_DIR/scripts/setup_cmake.sh"

echo "Building Boost..."
mkdir -p $PROJECT_ROOT/deps.install
# Fetch OpenSSL
echo "Fetch OpenSSL..."
cd "$PROJECT_ROOT/scripts"
./fetch_openssl.sh
./get_boost.sh
cd $PROJECT_ROOT


echo "Building libtorrent..."
$PROJECT_ROOT/scripts/build_libtorrent.sh
echo "Creating Prefab..."
$PROJECT_ROOT/scripts/create_prefab.sh
echo "Done"