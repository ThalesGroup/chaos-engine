#!/bin/sh

echo ${BRANCH}-$(cat git-chaos-engine/.git/short_ref) > version/tags.txt