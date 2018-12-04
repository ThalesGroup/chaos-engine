#!/bin/sh

echo ${BRANCH}-$(cat git-chaos-engine/.git/short_ref) > version/tags.txt

if [ $BRANCH = "master" ] ; then
  echo latest >> version/tags.txt
else
  echo ${BRANCH}-latest >> version/tags.txt
fi