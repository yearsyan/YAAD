#!/bin/bash

set -e
# Script to update ABI filters in app/build.gradle
# Usage: ./update_abi_filters.sh [abi_filter]
# Example: ./update_abi_filters.sh armeabi-v7a
# If no parameter is provided, defaults to 'arm64-v8a'

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Set default ABI filter
ABI_FILTER=${1:-"arm64-v8a"}

# Check if build.gradle exists
if [ ! -f "$PROJECT_ROOT/app/build.gradle" ]; then
    echo "Error: app/build.gradle file not found"
    exit 1
fi

# Detect platform and set sed backup option
if [[ "$OSTYPE" == "darwin"* ]]; then
    SED_OPTS=(-i '')
else
    SED_OPTS=(-i)
fi

sed "${SED_OPTS[@]}" '/^[[:space:]]*abiFilters[[:space:]]*/s/abiFilters.*/abiFilters "'"$ABI_FILTER"'"/' "$PROJECT_ROOT/app/build.gradle"
echo "Successfully updated ABI filters in app/build.gradle to '$ABI_FILTER'" 