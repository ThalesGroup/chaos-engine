#!/bin/bash

function start_engine(){
  ROOT_DIR=/tmp/chaosengine
  LIB=$ROOT_DIR/lib
  mkdir $ROOT_DIR
  mkdir $LIB

  cp -f $(find . | grep target | grep jar | grep -v entrypoint) $LIB
  cp -f $(find . | grep target | grep jar | grep entrypoint) $ROOT_DIR/chaosengine.jar
  cp ./chaosengine-launcher/src/main/resources/logback-spring.xml $LIB/

  rm $LIB/chaosengine-launcher*.jar
  rm $LIB/chaosengine-test-utilities-*.jar

  java -Djava.security.egd=file:/dev/./urandom -classpath $ROOT_DIR:$LIB/* -Dloader.path=$LIB -Dchaos.security.enabled=false -jar $ROOT_DIR/chaosengine.jar &
}

function wait_for_engine() {
     while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' $CHAOS_ENGINE_OPEN_API_URL )" != "200" ]]; do
      sleep 15;
      echo $(date) "Chaos Engine is not yet ready";
    done
}

function engine_startup_timeout(){
  echo $(date) "Chaos Engine container is not reachable after $CHAOS_ENGINE_STARTUP_TIMEOUT seconds"
  exit -1;
}

export -f wait_for_engine
export -f engine_startup_timeout

start_engine
timeout $CHAOS_ENGINE_STARTUP_TIMEOUT bash -c wait_for_engine || bash -c engine_startup_timeout
curl -s -o $OPEN_API_SPEC_FILE_NAME $CHAOS_ENGINE_OPEN_API_URL
kill $(pgrep java)
