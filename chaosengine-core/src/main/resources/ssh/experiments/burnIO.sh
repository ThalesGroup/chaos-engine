#!/bin/sh
# Dependencies: dd, df, grep, awk

TMP_FILESYSTEM=$(df /tmp -T | grep '/$' | awk '{print $2}')
FILE_NAME=chaos-burn-io-experiment

if [ "$TMP_FILESYSTEM" != "tmpfs" ]; then
  FILE_PATH=/tmp/$FILE_NAME
else
  FILE_PATH=$HOME/$FILE_NAME
fi

while true ; do
    dd if=/dev/zero of=$FILE_PATH bs=1M count=1024
done