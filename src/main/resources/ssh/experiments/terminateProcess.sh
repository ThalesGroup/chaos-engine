#!/bin/sh
# Description: Terminate random processes
# Dependencies: kill, cd, ls, grep, sort,  head


kill $(cd /proc;ls -1 | grep '[0-9]' |sort -R | head -1)