#!/bin/sh
#
# Install ClamAV, update definitions, and scan the current directory.
# Arguments:
#  $1 - The file path to save the scan log under

apk add --no-cache \
    clamav \
    rsyslog \
    wget \
    clamav-libunrar

/usr/bin/freshclam

/usr/bin/clamscan -vr . --log=$1