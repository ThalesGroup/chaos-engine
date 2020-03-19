#!/bin/sh
# Description: Simulates high CPU usage on all available processing units
# Dependencies: wc, yes, grep, sleep

limit=$(grep proc /proc/cpuinfo | wc -l)
counter=1

# This extra wait time is added in order to synchronize startup of parralel experiments
sleep 5

while [ $counter -le $limit ]; do
        yes "is it chaos ?" | grep "wow really?" &
        counter=$(($counter+1))
done