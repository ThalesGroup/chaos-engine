#!/bin/sh
# Dependencies: ip, sleep

# This extra wait time is added in order to synchronize startup of parralel experiments
sleep 5

ip route add blackhole 10.0.0.0/8