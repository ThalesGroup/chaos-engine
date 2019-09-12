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

package com.thales.chaos.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import com.thales.chaos.util.AwsMetadataUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ChaosHostIdentityProvider extends PropertyDefinerBase {
    private static final String KUBERNETES_NAMESPACE_FILE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    @Override
    public String getPropertyValue () {
        String personalIdentifier = getPersonalIdentifier();
        String networkIdentifier = getNetworkIdentifier();
        if (personalIdentifier == null && networkIdentifier == null) {
            return getHostName();
        }
        return String.join("@", personalIdentifier, networkIdentifier)
                     // If either are null, then strip them from the result
                     .replaceFirst("^(null@)?(.*?)(@null)?$", "$2");
    }

    String getPersonalIdentifier () {
        return Stream.<Supplier<String>>of(this::getKubernetesIdentifier, this::getHostName).map(Supplier::get)
                                                                                            .filter(Objects::nonNull)
                                                                                            .findFirst()
                                                                                            .orElse(null);
    }

    String getNetworkIdentifier () {
        return Stream.<Supplier<String>>of(this::getAwsIdentifier, this::getIpAddress).map(Supplier::get)
                                                                                      .filter(Objects::nonNull)
                                                                                      .findFirst()
                                                                                      .orElse(null);
    }

    String getHostName () {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }

    String getKubernetesIdentifier () {
        String namespace = getKubernetesNamespace();
        if (namespace == null) return null;
        String hostname = getHostName();
        if (hostname == null) return "k8s:" + namespace;
        return "k8s:" + hostname + "." + namespace;
    }

    String getAwsIdentifier () {
        AwsMetadataUtil.AwsInstanceIdentity awsInstanceIdentity = getAwsInstanceIdentity();
        if (awsInstanceIdentity == null) return null;
        return "aws:" + awsInstanceIdentity.getInstanceId() + "/" + awsInstanceIdentity.getAccountId();
    }

    String getIpAddress () {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }

    String getKubernetesNamespace () {
        try {
            return Files.readAllLines(Paths.get(KUBERNETES_NAMESPACE_FILE)).get(0);
        } catch (IOException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    AwsMetadataUtil.AwsInstanceIdentity getAwsInstanceIdentity () {
        return AwsMetadataUtil.getAwsInstanceIdentity();
    }
}
