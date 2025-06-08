#!/usr/bin/env bash
set -euo pipefail

# Usage: upload.sh <name> <file_path>
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <name> <file_path>"
  exit 1
fi

NAME="$1"
FILE="$2"

# Ensure environment variables are set
: "${UPLOAD_BASE:?Need to set UPLOAD_BASE}"
: "${UPLOAD_TOKEN:?Need to set UPLOAD_TOKEN}"

# Perform upload
response=$(curl -s -X POST "${UPLOAD_BASE}?name=${NAME}&token=${UPLOAD_TOKEN}" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@${FILE}")

# Extract URL from JSON
url=$(echo "$response" | jq -r '.data.url')

if [ -z "$url" ] || [ "$url" = "null" ]; then
  echo "Upload failed or URL not found in response:"
  echo "$response"
  exit 2
fi


# Generate and display QR code
if command -v qrencode >/dev/null 2>&1; then
  echo "QR code:"
  qrencode -t ANSIUTF8 "$url"
else
  echo "Install 'qrencode' to display a QR code in the terminal."
fi