/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.constants;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;

public abstract class AwsRDSConstants {
    public static final String AWS_RDS_AVAILABLE = "available";
    public static final String AWS_RDS_BACKING_UP = "backing-up";
    public static final String AWS_RDS_FAILING_OVER = "failing-over";
    public static final String AWS_RDS_PROMOTING = "promoting";
    public static final String AWS_RDS_CHAOS_SECURITY_GROUP = "ChaosEngine RDS Security Group";
    public static final String AWS_RDS_CHAOS_SECURITY_GROUP_DESCRIPTION = "(DO NOT USE) Security Group used by Chaos Engine to simulate random network failures.";
    public static final String AWS_RDS_CLUSTER_DATADOG_IDENTIFIER = "dbclusteridentifier";
    public static final String AWS_RDS_INSTANCE_DATADOG_IDENTIFIER = "dbinstanceidentifier";
    public static final String AWS_RDS_VPC_SECURITY_GROUP_ID = "vpcSecurityGroupId";
    public static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";
    public static final IpPermission DEFAULT_IP_PERMISSION = new IpPermission().withIpProtocol("-1").withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0"));

    private AwsRDSConstants () {
    }
    /*
    Sources:
    https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Aurora.Status.html
     */
}


