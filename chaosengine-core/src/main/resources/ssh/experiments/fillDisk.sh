#!/bin/sh
# Description: Fill 80% of remaining free space in system root partition
# Dependencies: dd, df, grep, awk

FREE_SPACE=$(($(df -amP | grep '/$' | awk '{print $4}') * 80 / 100))
dd if=/dev/zero of=/burn bs=1M count=$FREE_SPACE
