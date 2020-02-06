#!/bin/sh
# Description: Gradually allocates remaining free space in system root partition
# Dependencies: dd, df, grep, awk

TMP_FILESYSTEM=$(df /tmp -T | grep '/$' | awk '{print $2}')

if [ "$TMP_FILESYSTEM" != "tmpfs" ]; then
  FILE_PATH=/tmp/blob
else
  FILE_PATH=$HOME/blob
fi

FREE_SPACE=$(($(df -amPk | grep '/$' | awk '{print $4}') * 1024))
fallocate -l $(($FREE_SPACE * 99 / 100)) $FILE_PATH

FREE_SPACE=$(($(df -amPk | grep '/$' | awk '{print $4}')))
dd if=/dev/zero of=${FILE_PATH}2 bs=1K count=$FREE_SPACE
