#!/bin/sh
# Cattle:  true
# Description: Removes all DNS serves from system configuration and it makes sure that other system utils don't override this new settings
# Dependencies: echo, sleep

while true; do echo ""> /etc/resolv.conf; sleep 10; done