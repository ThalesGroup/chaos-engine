#!/bin/sh
# Cattle:  true
# Dependencies: nproc

for ((i = 0 ; i <= $(nproc) ; i++)); do
   yes "is it chaos ?" | grep "wow really?" &
done