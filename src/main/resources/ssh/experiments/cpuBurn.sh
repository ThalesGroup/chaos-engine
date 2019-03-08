#!/bin/sh
# Cattle:  true
# Dependencies: nproc

limit=$(nproc)
counter=1

while [ $counter -le $limit ]; do
        yes "is it chaos ?" | grep "wow really?" &
        let counter=$counter+1
done