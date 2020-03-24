#!/bin/sh
#
# Resolve MkDocs Dependencies

set -euxo pipefail

pip --cache-dir .pip install -r ./ci/docs/mkdocs_requirements.txt

VERSION=$(cat version)
$YEAR=$(date +%Y)

sed -i "s/<!-- year -->/$YEAR/g" docs/mkdocs.yml
sed -i "s/<!-- version -->/${VERSION:-Local Build}/g" docs/mkdocs.yml
