#!/bin/sh
# Description: Creates 65GiB file in system root partition
# Dependencies: dd

dd if=/dev/zero of=/burn bs=1M count=65536