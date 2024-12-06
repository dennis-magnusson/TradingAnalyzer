#!/bin/bash

mkdir -p ./data

URL="https://zenodo.org/records/6382482/files/debs2022-gc-trading-day-12-11-21.csv?download=1"
DEST="./data/debs2022-gc-trading-day-12-11-21.csv"

curl -L -o "$DEST" "$URL"

if [ $? -eq 0 ]; then
    echo "File downloaded successfully and saved to $DEST"
else
    echo "Failed to download the file."
    exit 1
fi
