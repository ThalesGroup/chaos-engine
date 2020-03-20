#!/bin/sh
# Description: Removes all DNS serves from system configuration and it makes sure that other system utils don't override this new settings
# Dependencies: echo, sleep

# This extra wait time is added in order to synchronize startup of parralel experiments
sleep 5

while true; do echo ""> /etc/resolv.conf; sleep 10; done