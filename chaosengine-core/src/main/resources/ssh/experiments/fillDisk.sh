#!/bin/sh
# Description: Gradualy allocates remaining free space in system root partition
# Dependencies: dd, df, grep, awk, sleep



FREE_SPACE=$(($(df -amPk | grep '/$' | awk '{print $4}')*1024))

fallocate -l $(($FREE_SPACE*25/100)) /blob
sleep 60
fallocate -l $(($FREE_SPACE*50/100)) /blob
sleep 60
fallocate -l $(($FREE_SPACE*75/100)) /blob
sleep 60
fallocate -l $FREE_SPACE /blob
