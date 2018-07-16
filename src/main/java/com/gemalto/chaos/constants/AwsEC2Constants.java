package com.gemalto.chaos.constants;

public abstract class AwsEC2Constants {
    public static final int AWS_PENDING_CODE = 0;
    public static final int AWS_RUNNING_CODE = 16;
    public static final int AWS_SHUTTING_DOWN_CODE = 32;
    public static final int AWS_STOPPING_CODE = 64;
    public static final int AWS_TERMINATED_CODE = 48;
    public static final int AWS_STOPPED_CODE = 80;

    private AwsEC2Constants () {
    }

    public static int[] getAwsUnhealthyCodes () {
        return new int[]{ AWS_PENDING_CODE, AWS_SHUTTING_DOWN_CODE, AWS_STOPPING_CODE, AWS_STOPPED_CODE, AWS_STOPPED_CODE };
    }
}
