#!/bin/sh
# Cattle:  true
# Dependencies: dd, while


while [[ true ]] ; do
    dd if=/dev/urandom of=/burn bs=1M count=1024
done