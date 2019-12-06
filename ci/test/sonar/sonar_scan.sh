#!/bin/bash
# The script executes Sonar scan using Maven and then waits for Sonar Quality gate results
# It will fail the GitLab job if the gate is read or new bugs or vulnerabilities were detected

#Exit script immediately on any error in it
set -eo pipefail

apt update
apt install -y jq curl

SCAN_LOG_FILE=sonar_scan.log

mvn $MAVEN_CLI_OPTS sonar:sonar -Dsonar.projectKey="$SONAR_PROJECT_KEY" -Dsonar.host.url="$SONAR_URL" -Dsonar.login="$SONAR_TOKEN" | tee $SCAN_LOG_FILE
SCAN_STATUS_URL=$(grep 'More about the report processing at' $SCAN_LOG_FILE | awk '{print $8}')

while [ "$SCAN_STATUS" != "SUCCESS" ]; do
  echo "Sonar server is processing the scan, actual status $SCAN_STATUS"
  SCAN_STATUS=$(curl -s $SCAN_STATUS_URL | jq -r '.task.status')
  sleep 5
done

QUALITY_GATE_STATUS=$(curl -s "$SONAR_URL/api/project_branches/list?project=$SONAR_PROJECT_KEY" | jq -r '.branches|.[]|select(.name=="master")|.status.qualityGateStatus')
ALERT_STATUS=$(curl -s "$SONAR_URL/api/measures/search?projectKeys=$SONAR_PROJECT_KEY&metricKeys=alert_status" | jq -r '.[]|.[] .value')
BUG_COUNT=$(curl -s "$SONAR_URL/api/measures/search?projectKeys=$SONAR_PROJECT_KEY&metricKeys=bugs" | jq -r '.[]|.[] .value')
VULN_COUNT=$(curl -s "$SONAR_URL/api/measures/search?projectKeys=$SONAR_PROJECT_KEY&metricKeys=vulnerabilities" | jq -r '.[]|.[] .value')
CODE_COVERAGE=$(curl -s "$SONAR_URL/api/measures/search?projectKeys=$SONAR_PROJECT_KEY&metricKeys=coverage" | jq -r '.[]|.[] .value')

echo "Code Coverage: $CODE_COVERAGE"

if [[ "$QUALITY_GATE_STATUS" != "OK" || "$ALERT_STATUS" != "OK" || "$BUG_COUNT" != "0" || "$VULN_COUNT" != "0" ]]; then
  echo "Sonar has detected issues"
  echo "Quality gate status: $QUALITY_GATE_STATUS"
  echo "Alert status: $ALERT_STATUS"
  echo "Bugs detected: $BUG_COUNT"
  echo "Vulnerabilities: $VULN_COUNT"
  exit 1
fi
echo "Sonar scan passed"
