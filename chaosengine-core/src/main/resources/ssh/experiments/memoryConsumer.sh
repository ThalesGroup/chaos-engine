#!/bin/sh
# Description: Tie up memory in other processes for some time, allowing
# Dependencies: cat, dd, sleep, grep, awk

MEM_TOTAL=$(grep MemTotal /proc/meminfo  | awk ' { print $2 } ')
MEM_TOTAL=$((${MEM_TOTAL}*1024))

MEM_FREE=$(($(cat /sys/fs/cgroup/memory/memory.limit_in_bytes) - $(cat /sys/fs/cgroup/memory/memory.usage_in_bytes) - 1024*1024))

if [ "$MEM_FREE" -gt "$MEM_TOTAL" ] ; then
    MEM_FREE=$(grep MemAvailable /proc/meminfo  | awk ' { print $2 } ')
    MEM_FREE=$((${MEM_FREE} * 1024))
fi

dd if=/dev/zero of=/dev/stdout bs=${MEM_FREE} count=1 | sleep 99999