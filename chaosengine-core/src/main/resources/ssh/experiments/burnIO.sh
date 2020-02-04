#!/bin/sh
# Dependencies: dd

if [ "$TMP_FILESYSTEM" != "tmpfs" ]; then
  FILE_PATH=/tmp/blob
else
  FILE_PATH=$HOME/blob
fi

while true ; do
    dd if=/dev/zero of=$FILE_PATH bs=1M count=1024
done