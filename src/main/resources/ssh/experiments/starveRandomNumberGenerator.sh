#!/bin/sh
# Cattle: true
# Dependencies: dd

while [[ true ]] ; do
  dd if=/dev/random of=/dev/null bs=1 count=1024;
done