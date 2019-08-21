#!/bin/sh
#
# Build MkDocs
#
# Arguments:
#  $1 - Build Path (default 'public')

set -euxo pipefail

pip install -r ./ci/docs/mkdocs_requirements.txt

mkdocs build --site-dir ${1-public}