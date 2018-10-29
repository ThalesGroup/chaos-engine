package com.gemalto.chaos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwsRDSUtils {
    private static final Pattern chaosSnapshotPattern = Pattern.compile("ChaosSnapshot-(.*)-([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{0,3}Z)");
    private static final Logger log = LoggerFactory.getLogger(AwsRDSUtils.class);

    private AwsRDSUtils () {
    }

    public static String generateSnapshotName (String identifier) {
        return String.format("ChaosSnapshot-%s-%s", identifier, Instant.now());
    }

    public static boolean isChaosSnapshot (String snapshotIdentifier) {
        Matcher matcher = chaosSnapshotPattern.matcher(snapshotIdentifier);
        if (matcher.find()) {
            if (log.isDebugEnabled()) {
                try {
                    String instanceName = matcher.group(1);
                    Instant snapshotTime = Instant.parse(matcher.group(2));
                    log.debug("Found snapshot for {} created at {}", instanceName, snapshotTime);
                } catch (Exception e) {
                    log.error("{} appears to be a Chaos Snapshot, but could not parse it", e);
                }
            }
            return true;
        }
        return false;
    }
}
