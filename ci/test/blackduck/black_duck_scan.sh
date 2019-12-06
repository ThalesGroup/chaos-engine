#!/bin/bash

bash <(curl -s -L https://detect.synopsys.com/detect.sh) \
    --blackduck.url=$BLACK_DUCK_URL \
    --detect.project.name="$BLACK_DUCK_PROJECT_NAME" \
    --blackduck.api.token="$BLACK_DUCK_TOKEN" \
    --detect.policy.check.fail.on.severities="$BLACK_DUCK_FAIL_ON_SEVERITIES" \
    --detect.report.timeout=$BLACK_DUCK_TIMEOUT
