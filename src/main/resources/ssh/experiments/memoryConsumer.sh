#!/bin/sh
# Description: Tie up memory in other processes for some time, allowing
# Dependencies: cat, dd, sleep
# Cattle: true

MEM_FREE=$(($(cat /sys/fs/cgroup/memory/memory.limit_in_bytes) - $(cat /sys/fs/cgroup/memory/memory.usage_in_bytes) - 1024*1024))

dd if=/dev/zero of=/dev/stdout bs=${MEM_FREE} count=1 | sleep 99999