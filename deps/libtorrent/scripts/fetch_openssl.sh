#!/bin/bash

# Exit on error
set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Set default OpenSSL version if not specified
OPENSSL_VERSION=${OPENSSL_VERSION:-"3.0.15-4"}

# Set up PATH for cross-compilation
export PATH=$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH

# Create build and install directories
mkdir -p "$PROJECT_ROOT/build/install"

# Function to build for a specific architecture
fetch_arch() {
    local arch=$1
    local abi_name=$2
    local api_level=24
    local install_dir="$PROJECT_ROOT/deps.install/$abi_name"

    mkdir -p $install_dir
    echo "Fetch OpenSSL for $arch ($abi_name)..."
    wget "https://github.com/beeware/cpython-android-source-deps/releases/download/openssl-$OPENSSL_VERSION/openssl-$OPENSSL_VERSION-$arch-linux-android.tar.gz" -O "$PROJECT_ROOT/openssl-$OPENSSL_VERSION-$arch.tar.gz"
    tar -xzf "$PROJECT_ROOT/openssl-$OPENSSL_VERSION-$arch.tar.gz" -C "$install_dir"
    rm -rf "$PROJECT_ROOT/openssl-$OPENSSL_VERSION-$arch.tar.gz"

    # Navigate to OpenSSL directory
    cd "$OPENSSL_SOURCE_DIR"

    echo "OpenSSL has been built and installed to $install_dir directory"
}

# Build for all architectures
# build_arch "arm" "armeabi-v7a"
fetch_arch "aarch64" "arm64-v8a"
# build_arch "x86" "x86"
fetch_arch "x86_64" "x86_64"

echo "OpenSSL has been built for all architectures in $PROJECT_ROOT/build/deps.install directory"
