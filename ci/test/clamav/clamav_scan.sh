#!/bin/sh
#
# Install ClamAV, update definitions, and scan the current directory.
# Arguments:
#  $1 - The file path to save the scan log under
#  $2 - The directory path to save the definition files

set -euxo pipefail

mkdir -p ${2-.clamav/}

chown 100:101 ${2-.clamav/}

apk add --no-cache \
    clamav \
    rsyslog \
    wget \
    clamav-libunrar

/usr/bin/freshclam --datadir=${2-.clamav/}

/usr/bin/clamscan -d ${2-.clamav/} -vr . --log=${1-av.log}