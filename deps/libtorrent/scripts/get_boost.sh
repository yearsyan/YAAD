#!/bin/bash

# Exit on error
set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd $PROJECT_ROOT

get_arch() {
    local vcpkg_triplet=$1
    local abi_name=$2
    local api_level=24
    local install_dir="$PROJECT_ROOT/deps.install/$abi_name"

    mkdir -p $install_dir
    mkdir -p $install_dir/include
    mkdir -p $install_dir/lib
    vcpkg  install --triplet $vcpkg_triplet
    cp -r $PROJECT_ROOT/vcpkg_installed/$vcpkg_triplet/include/* $install_dir/include/
    cp -r $PROJECT_ROOT/vcpkg_installed/$vcpkg_triplet/lib/* $install_dir/lib/
}

get_arch "arm64-android" "arm64-v8a"
get_arch "x64-android" "x86_64"
echo "Boost has been built for all architectures in $PROJECT_ROOT/build/deps.install directory"