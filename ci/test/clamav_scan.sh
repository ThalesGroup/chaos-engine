#!/bin/sh
#
# Install ClamAV, update definitions, and scan the current directory.
# Arguments:
#  $1 - The file path to save the scan log under
#  $2 - The directory path to save the definition files

mkdir -p $2

chown 100:101 $2

apk add --no-cache \
    clamav \
    rsyslog \
    wget \
    clamav-libunrar

/usr/bin/freshclam --datadir=$2

/usr/bin/clamscan -d $2 -vr . --log=$1