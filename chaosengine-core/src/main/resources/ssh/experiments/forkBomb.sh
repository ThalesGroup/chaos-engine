#!/bin/sh
# Description: Fork Bomb experiment to consume CPU
# Dependencies: sleep

# This extra wait time is added in order to synchronize startup of parallel experiments
sleep 5

bomb() { bomb | bomb & }; bomb;