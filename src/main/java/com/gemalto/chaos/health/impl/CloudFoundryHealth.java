package com.gemalto.chaos.health.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("CF_INSTANCE_UUID")
public class CloudFoundryHealth {

}
