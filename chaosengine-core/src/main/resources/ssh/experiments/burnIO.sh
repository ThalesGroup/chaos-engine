#!/bin/sh
# Dependencies: dd


while true ; do
    dd if=/dev/zero of=/tmp/burn bs=1M count=1024
done