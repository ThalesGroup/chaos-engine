#!/bin/sh
# Description: Simulates high CPU usage on all available processing units
# Dependencies: wc, yes, grep

limit=$(grep proc /proc/cpuinfo | wc -l)
counter=1

while [ $counter -le $limit ]; do
        yes "is it chaos ?" | grep "wow really?" &
        counter=$(($counter+1))
done