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

package com.thales.chaos.platform.enums;

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

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters () {
        return Arrays.asList(new Object[]{ "ReplicationController", ControllerKind.REPLICATION_CONTROLLER }, new Object[]{ "ReplicaSet", ControllerKind.REPLICA_SET }, new Object[]{ "Deployment", ControllerKind.DEPLOYMENT }, new Object[]{ "StatefulSet", ControllerKind.STATEFUL_SET }, new Object[]{ "DaemonSet", ControllerKind.DAEMON_SET }, new Object[]{ "Job", ControllerKind.JOB }, new Object[]{ "CronJob", ControllerKind.CRON_JOB }, new Object[]{ "Bogus", ControllerKind.UNKNOWN });
    }

    @Test
    public void mapFromString () {
        assertSame("Error converting to Enum from " + rawName, enumName, ControllerKind.mapFromString(rawName));
    }
}