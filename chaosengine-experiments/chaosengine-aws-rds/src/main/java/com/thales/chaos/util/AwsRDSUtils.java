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

package com.thales.chaos.util;

import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.exception.enums.AwsChaosErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwsRDSUtils {
    private static final Pattern chaosSnapshotPattern = Pattern.compile("ChaosSnapshot-(.*)-([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}-[0-9]{2}-[0-9]{2}(-[0-9]{0,3})?Z)", Pattern.CASE_INSENSITIVE);
    private static final Logger log = LoggerFactory.getLogger(AwsRDSUtils.class);
    private static final DateTimeFormatter reverseTimestampFormatter = new DateTimeFormatterBuilder().parseLenient()
                                                                                                     .parseCaseInsensitive()
                                                                                                     .appendPattern("yyyy-MM-dd")
                                                                                                     .appendLiteral('T')
                                                                                                     .appendPattern("HH-mm-ss")
                                                                                                     .appendPattern("[-SSS]")
                                                                                                     .appendZoneId()
                                                                                                     .toFormatter();

    private AwsRDSUtils () {
    }

    public static String generateSnapshotName (@NotNull String identifier) {
        String timestamp = new DateTimeFormatterBuilder().appendInstant(3).toFormatter().format(Instant.now());
        String output = String.format("ChaosSnapshot-%s-%s", identifier, timestamp).replace(":", "-").replace(",", "-")
                              .replaceAll("\\.", "-")
                              .replaceAll("--+", "-");
        int identifierTrim = 0;
        while (!isValidSnapshotName(output)) {
            output = String.format("ChaosSnapshot-%s-%s",
                    identifier.substring(0, identifier.length() - ++identifierTrim),
                    timestamp).replace(":", "-").replace(",", "-")
                           .replaceAll("\\.", "-")
                           .replaceAll("--+", "-");
            if (identifierTrim == identifier.length()) {
                /*
                I could not find a way to trigger this! Exception is here just in case, but I can't find a way to
                make this happen.
                */
                throw new ChaosException(AwsChaosErrorCode.INVALID_SNAPSHOT_NAME);
            }
        }
        return output;
    }

    static boolean isValidSnapshotName (@NotNull String s) {
        return s.length() <= 255 && s.length() > 0 && !s.contains("--") && !s.endsWith("-");
    }

    public static boolean isChaosSnapshot (String snapshotIdentifier) {
        Matcher matcher = chaosSnapshotPattern.matcher(snapshotIdentifier);
        if (matcher.find()) {
            log.debug("{} appears to be generated from Chaos. Deleting is allowed.", snapshotIdentifier);
            return true;
        }
        return false;
    }

    public static Instant getInstantFromNameSegment (String segment) {
        return reverseTimestampFormatter.parse(segment, Instant::from);
    }
}
