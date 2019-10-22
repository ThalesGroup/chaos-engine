#!/bin/bash

anchore-cli system wait
anchore-cli registry add "$CI_REGISTRY" $CI_REGISTRY_USER $CI_REGISTRY_PASSWORD --skip-validate
/opt/rh/rh-python36/root/usr/bin/python3 /usr/local/bin/anchore_ci_tools.py -a -r --type vuln --vuln $ANCHORE_SCAN_TYPE --image $DOCKER_IMAGE_NAME --timeout $ANCHORE_TIMEOUT

for f in anchore-reports/*; do
  if [[ "$f" =~ "vuln.json" ]]; then
    echo "Vulnerabilities summary:"
    jq --raw-output '[.vulnerabilities|group_by(.severity)|.[]| {severity: .[0].severity, vuln: [.[].vuln]|length}]|.[] | "\(.severity) \(.vuln)"' $f
    CRITICAL=$(jq '.vulnerabilities| sort_by(.severity)|.[]|select(.severity=="Critical")| .url' $f | wc -l)
    HIGH=$(jq '.vulnerabilities| sort_by(.severity)|.[]|select(.severity=="High")| .url' $f | wc -l)
  fi
done


if [[ "$CRITICAL" != "0" || "$HIGH" != "0" ]]; then
  echo "Scan failed, critial or high vulnerabilities detected"
  exit 1
fi

echo "Scan passed"