#!/bin/sh
# Description: Terminate random processes
# Dependencies: for, kill, sleep, ps, grep, awk, readlink, set

if readlink -f /proc/1/exe | grep -q -e systemd -e init; then
  ps -A -o pid,comm | grep -i \
    -e docker \
    -e java \
    -e python \
    -e mysql \
    -e cassandra \
    -e node \
    -e etcd \
    -e mongod \
    -e nginx \
    -e httpd \
    -e postgres |
    awk ' { print $1 } ' |
    xargs kill -9

else
  for _ in 1 2 3 4 5; do
    kill -2 1
    sleep 2s
  done
fi
