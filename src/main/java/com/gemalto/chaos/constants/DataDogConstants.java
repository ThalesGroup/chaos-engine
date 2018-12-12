package com.gemalto.chaos.constants;

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
