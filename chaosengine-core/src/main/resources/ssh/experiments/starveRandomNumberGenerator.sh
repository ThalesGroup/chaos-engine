#!/bin/sh
# Dependencies: dd, sleep

# This extra wait time is added in order to synchronize startup of parralel experiments
sleep 5

while true ; do
  dd if=/dev/random of=/dev/null bs=1 count=1024;
done