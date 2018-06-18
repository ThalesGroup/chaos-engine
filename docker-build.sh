#!/usr/bin/env bash

docker volume ls -q | grep maven-repo || docker volume create maven-repo

docker run -it --rm -v "$PWD":/usr/src/mymaven -v maven-repo:/root/.m2 -v "$PWD/target:/usr/src/mymaven/target" -w /usr/src/mymaven maven:3.5-alpine mvn install package test
