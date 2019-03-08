#!/bin/sh
# Description: Terminate random processes
# Dependencies: kill, ls, grep, sort,  head
# Cattle: true


kill $(cd /proc;ls -1 | grep '[0-9]' |sort -R | head -1)