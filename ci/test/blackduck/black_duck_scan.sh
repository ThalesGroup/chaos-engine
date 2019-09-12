#!/bin/bash

bash <(curl -s -L https://detect.synopsys.com/detect.sh) \
    --blackduck.url=$BLACK_DUCK_URL \
    --detect.project.name="$BLACK_DUCK_PROJECT_NAME" \
    --blackduck.api.token="$BLACK_DUCK_TOKEN" \
    --detect.policy.check.fail.on.severities="CRITICAL,BLOCKER" \
    --detect.report.timeout=600
