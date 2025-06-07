#!/bin/bash

# Script to update ABI filters in app/build.gradle
# Usage: ./update_abi_filters.sh [abi_filter]
# Example: ./update_abi_filters.sh armeabi-v7a
# If no parameter is provided, defaults to 'arm64-v8a'

# Set default ABI filter
ABI_FILTER=${1:-"arm64-v8a"}

# Check if build.gradle exists
if [ ! -f "app/build.gradle" ]; then
    echo "Error: app/build.gradle file not found"
    exit 1
fi

# Update the ABI filters
sed -i '' "s/abiFilters .arm64-v8a., .x86_64./abiFilters '$ABI_FILTER'/" app/build.gradle

echo "Successfully updated ABI filters in app/build.gradle to '$ABI_FILTER'" 