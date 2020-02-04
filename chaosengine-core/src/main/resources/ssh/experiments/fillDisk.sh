#!/bin/sh
# Description: Gradualy allocates remaining free space in system root partition
# Dependencies: dd, df, grep, awk, sleep

TMP_FILESYSTEM=$(df /tmp -T | grep '/$' | awk '{print $2}')

if [ "$TMP_FILESYSTEM" != "tmpfs" ]; then
  FILE_PATH=/tmp/blob
else
  FILE_PATH=$HOME/blob
fi

FREE_SPACE=$(($(df -amPk | grep '/$' | awk '{print $4}') * 1024))

fallocate -l $(($FREE_SPACE * 25 / 100)) $FILE_PATH
sleep 60
fallocate -l $(($FREE_SPACE * 50 / 100)) $FILE_PATH
sleep 60
fallocate -l $(($FREE_SPACE * 75 / 100)) $FILE_PATH
sleep 60
fallocate -l $FREE_SPACE $FILE_PATH
