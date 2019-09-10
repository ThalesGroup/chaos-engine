/*
 *    Copyright (c) 2019 Thales Group
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

public class DataDogConstants {
    public static final String DEFAULT_DATADOG_IDENTIFIER_KEY = "host";
    public static final String AVAILABILITY_ZONE = "availability-zone";
    public static final String DATADOG_PLATFORM_KEY = "platform";
    public static final String DATADOG_CONTAINER_KEY = "container";
    public static final String DATADOG_EXPERIMENTID_KEY="experimentid";
    public static final String SLACK_NOTIFICATION_SERVER_RESPONSE_KEY ="slack-server-response";
    public static final String RDS_INSTANCE_ID = "instanceid";
    public static final String RDS_INSTANCE_SNAPSHOT = "dbsnapshot";
    public static final String RDS_CLUSTER_SNAPSHOT = "dbclustersnapshot";
    public static final String EC2_INSTANCE = "ec2-instance";
    public static final String DATADOG_EXPERIMENT_METHOD_KEY = "experimentMethod";

    private DataDogConstants () {
    }
}
