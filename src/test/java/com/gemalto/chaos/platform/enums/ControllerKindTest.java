package com.gemalto.chaos.platform.enums;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertSame;

@RunWith(Parameterized.class)
public class ControllerKindTest {
    private final String rawName;
    private final ControllerKind enumName;

    public ControllerKindTest (String rawName, ControllerKind enumName) {
        this.rawName = rawName;
        this.enumName = enumName;
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters () {
        return Arrays.asList(new Object[]{ "ReplicationController", ControllerKind.REPLICATION_CONTROLLER }, new Object[]{ "ReplicaSet", ControllerKind.REPLICA_SET }, new Object[]{ "Deployment", ControllerKind.DEPLOYMENT }, new Object[]{ "StatefulSet", ControllerKind.STATEFUL_SET }, new Object[]{ "DaemonSet", ControllerKind.DAEMON_SET }, new Object[]{ "Job", ControllerKind.JOB }, new Object[]{ "CronJob", ControllerKind.CRON_JOB }, new Object[]{ "Bogus", null });
    }

    @Test
    public void mapFromString () {
        assertSame("Error converting to Enum from " + rawName, enumName, ControllerKind.mapFromString(rawName));
    }
}