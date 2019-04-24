package com.gemalto.chaos.container;

public abstract class AwsContainer extends Container {
    protected String availabilityZone;

    public String getAvailabilityZone () {
        return availabilityZone;
    }
}
