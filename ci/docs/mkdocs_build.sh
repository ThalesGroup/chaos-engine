#!/bin/sh
#
# Build MkDocs
#
# Arguments:
#  $1 - Build Path (default 'public')

set -euxo pipefail

mkdocs build --config-file docs/mkdocs.yml --site-dir "${1-public}"
mv docs/"${1-public}" "${2-$(pwd)}"