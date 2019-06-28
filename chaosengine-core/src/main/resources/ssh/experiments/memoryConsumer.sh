#!/bin/sh
# Description: Tie up memory in other processes for some time, allowing
# Dependencies: cat, dd, sleep, grep, awk

MEM_TOTAL=$(grep MemTotal /proc/meminfo  | awk ' { print $2 } ')
MEM_TOTAL=$((${MEM_TOTAL}*1024))

MEM_AVAILABLE=$(grep MemAvailable /proc/meminfo  | awk ' { print $2 } ')
MEM_AVAILABLE=$((${MEM_AVAILABLE} * 1024))

CMEMLIMITS=/sys/fs/cgroup/memory/memory.limit_in_bytes
CMEMUSAGE=/sys/fs/cgroup/memory/memory.usage_in_bytes

if [ -f $CMEMLIMITS ] && [ -f $CMEMUSAGE ]; then
   MEM_FREE=$(($(cat ${CMEMLIMITS}) - $(cat ${CMEMUSAGE}) - 1024*1024))
    if [ "$MEM_FREE" -gt "$MEM_TOTAL" ] ; then
        MEM_FREE=${MEM_AVAILABLE}
    fi
else
    MEM_FREE=${MEM_AVAILABLE}
fi

dd if=/dev/zero of=/dev/stdout bs=${MEM_FREE} count=1 | sleep 99999