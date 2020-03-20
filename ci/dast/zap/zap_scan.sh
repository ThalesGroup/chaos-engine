#!/bin/bash

function run_scan() {
    /zap/zap-api-scan.py -t $CHAOS_ENGINE_OPEN_API_URL -f openapi -r $ZAP_REPORT_FILE
    SCAN_STATUS=$?
    echo "Scan exited with status:" $SCAN_STATUS
    if [ $SCAN_STATUS -eq 0 ]; then
      echo "Scan PASSED"
      return 0
    elif [ $SCAN_STATUS -eq 1 ]; then
      echo "Scan FAILED"
      return 1
    elif [ $SCAN_STATUS -eq 2 ]; then
      echo "Scan PASSED with WARINGS"
      return 0
    fi
    echo "Scan retuned undefined error"
    return $SCAN_STATUS
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

mkdir $ZAP_WORK_DIR
export -f wait_for_engine
timeout $CHAOS_ENGINE_STARTUP_TIMEOUT bash -c wait_for_engine || engine_startup_timeout
run_scan
STATUS_CODE=$?

if [[ -d "/report" ]]; then
  #GitHub artifacts must be in directory allowing rw access to everyone
  cp $ZAP_WORK_DIR$ZAP_REPORT_FILE /report
else
  #GitLab artifacts must be in a cloned repo
  cp $ZAP_WORK_DIR$ZAP_REPORT_FILE .
fi


exit $STATUS_CODE