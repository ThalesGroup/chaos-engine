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

package com.thales.chaos.util;

import org.junit.Test;

import java.time.Instant;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;

public class AwsRDSUtilsTest {
    @Test
    public void generateSnapshotName () {
        IntStream.range(0, 100).parallel().forEach(i -> {
            String identifier = randomUUID().toString();
            String snapshotName = AwsRDSUtils.generateSnapshotName(identifier);
            assertTrue(AwsRDSUtils.isChaosSnapshot(snapshotName));
        });
        IntStream.range(0, 100).parallel().forEach(i -> {
            String identifier = randomUUID().toString() + "-";
            String snapshotName = AwsRDSUtils.generateSnapshotName(identifier);
            assertTrue(AwsRDSUtils.isChaosSnapshot(snapshotName));
        });
        IntStream.range(0, 100).parallel().forEach(i -> {
            String identifier = randomUUID().toString() + "--" + randomUUID().toString();
            String snapshotName = AwsRDSUtils.generateSnapshotName(identifier);
            assertTrue(AwsRDSUtils.isChaosSnapshot(snapshotName));
        });
        IntStream.range(0, 100).parallel().forEach(i -> {
            StringBuilder identifier = new StringBuilder(randomUUID().toString());
            while (identifier.length() < 300) {
                identifier.append(randomUUID().toString());
            }
            String snapshotName = AwsRDSUtils.generateSnapshotName(identifier.toString());
            assertTrue(AwsRDSUtils.isChaosSnapshot(snapshotName));
        });
    }

    @Test
    public void rdsSnapshotNameConstraints () {

        /*
        https://docs.aws.amazon.com/cli/latest/reference/rds/create-db-snapshot.html

            Constraints:

            Can't be null, empty, or blank
            Must contain from 1 to 255 letters, numbers, or hyphens
            First character must be a letter
            Can't end with a hyphen or contain two consecutive hyphens
         */
        IntStream.range(0, 100).parallel().mapToObj(i -> AwsRDSUtils.generateSnapshotName(randomUUID().toString())).forEach(s -> {
            assertTrue("Snapshot name cannot exceed 255 letters", s.length() <= 255);
            assertNotNull("Snapshot name cannot be null", s);
            assertNotEquals("Snapshot name cannot be blank", "", s);
            assertFalse("Cannot contain two consecutive hypens", s.contains("--"));
            assertFalse("Cannot end with a hypen", s.endsWith("-"));
            assertTrue("Unexpected name pattern (Can contain letters, numbers, and hyphens)" + s, Pattern.compile("^[A-Za-z0-9-]+$")
                                                                                                         .matcher(s)
                                                                                                         .matches());
        });
    }

    @Test
    public void getInstantFromNameSegment () {
        assertEquals(Instant.EPOCH, AwsRDSUtils.getInstantFromNameSegment("1970-01-01T00-00-00Z"));
        assertEquals(Instant.ofEpochSecond(1540828376), AwsRDSUtils.getInstantFromNameSegment("2018-10-29T15-52-56Z"));
        assertEquals(Instant.ofEpochMilli(1540828376123L), AwsRDSUtils.getInstantFromNameSegment("2018-10-29T15-52-56-123Z"));
        assertEquals(Instant.ofEpochMilli(1540828376120L), AwsRDSUtils.getInstantFromNameSegment("2018-10-29T15-52-56-12Z"));
        assertEquals(Instant.ofEpochMilli(1540828376100L), AwsRDSUtils.getInstantFromNameSegment("2018-10-29T15-52-56-1Z"));
    }

    @Test
    public void isChaosSnapshot () {
        assertFalse("Should not pick up a normal snapshot", AwsRDSUtils.isChaosSnapshot("Just-another-everyday-snapshot"));
        assertTrue("Should pick up a snapshot named correctly", AwsRDSUtils.isChaosSnapshot("ChaosSnapshot-ASJKLFJAK-2018-01-01T12-34-56-123Z"));
    }

    @Test
    public void isValidSnapshotName () {
        StringBuilder longString = new StringBuilder("ChaosSnapshot");
        IntStream.range(0, 300).forEach(longString::append);
        longString.append("2018-01-01T01-01-01Z");
        assertFalse("Snapshot name is too long", AwsRDSUtils.isValidSnapshotName(longString.toString()));
        assertFalse("Snapshot name is too short", AwsRDSUtils.isValidSnapshotName(""));
        assertFalse("Snapshot name contains --", AwsRDSUtils.isValidSnapshotName("ChaosSnapshot--Instance-1970-01-01T01-01-01Z"));
        assertFalse("Snapshot name ends with -", AwsRDSUtils.isValidSnapshotName("ChaosSnapshot-Instance-1970-01-01T01-01-01Z-"));
        assertTrue("Expected good snapshot name", AwsRDSUtils.isValidSnapshotName("ChaosSnapshot-Instance-1970-01-01T01-01-01Z"));
        assertTrue("Expected good snapshot name", AwsRDSUtils.isValidSnapshotName("ChaosSnapshot-Instance-1970-01-01T01-01-01-123Z"));
    }
}