#!/bin/sh
#
# Resolve MkDocs Dependencies

set -euxo pipefail

pip --cache-dir .pip install -r ./ci/docs/mkdocs_requirements.txt

VERSION=$(cat version)

sed -i "s/<!-- version -->/${VERSION:-Local Build}/g" docs/mkdocs.yml
