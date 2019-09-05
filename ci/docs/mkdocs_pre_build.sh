#!/bin/sh
#
# Resolve MkDocs Dependencies

set -euxo pipefail

pip install -r ./ci/docs/mkdocs_requirements.txt

