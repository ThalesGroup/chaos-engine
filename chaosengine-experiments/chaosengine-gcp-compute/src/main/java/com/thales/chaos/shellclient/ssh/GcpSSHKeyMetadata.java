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

package com.thales.chaos.shellclient.ssh;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@JsonIgnoreType
public class GcpSSHKeyMetadata {
    private static final String[] patterns;

    static {
        patterns = new String[]{
                "^(?<username>.*?):ssh-rsa (?<publicKey>.*?) (?<identifier>.*?)$"
        };
    }

    private String username;
    private String publicKey;
    private String identifier;

    GcpSSHKeyMetadata (String username, String publicKey, String identifier) {
        this.username = Objects.requireNonNull(username);
        this.publicKey = Objects.requireNonNull(publicKey);
        this.identifier = Objects.requireNonNull(identifier);
    }

    public static List<GcpSSHKeyMetadata> parseMetadata (String originalMetadata) {
        String[] split = originalMetadata.split("\n");
        return Arrays.stream(split)
                     .filter(Objects::nonNull)
                     .filter(not(String::isBlank))
                     .map(String::strip)
                     .map(GcpSSHKeyMetadata::parseIndividualMetadata)
                     .collect(Collectors.toList());
    }

    private static GcpSSHKeyMetadata parseIndividualMetadata (String originalMetadataSection) {
        for (String pattern : patterns) {
            if (originalMetadataSection.matches(pattern)) {
                Matcher matcher = Pattern.compile(pattern).matcher(originalMetadataSection);
                if (!matcher.find()) return null;
                String username = matcher.group("username");
                String publicKey = matcher.group("publicKey");
                String identifier = matcher.group("identifier");
                return new GcpSSHKeyMetadata(username, publicKey, identifier);
            }
        }
        return null;
    }

    public static String metadataFormat (Collection<GcpSSHKeyMetadata> metadataCollection) {
        return metadataCollection.stream()
                                 .map(GcpSSHKeyMetadata::metadataFormat)
                                 .collect(Collectors.joining("\n", "", "\n"));
    }

    private String metadataFormat () {
        return String.format("%s:ssh-rsa %s %s", username, publicKey, identifier);
    }

    public String getUsername () {
        return username;
    }

    public String getPublicKey () {
        return publicKey;
    }

    public String getIdentifier () {
        return identifier;
    }

    @Override
    public int hashCode () {
        return Objects.hash(username, publicKey, identifier);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GcpSSHKeyMetadata that = (GcpSSHKeyMetadata) o;
        return Objects.equals(username, that.username) && Objects.equals(publicKey, that.publicKey) && Objects.equals(
                identifier,
                that.identifier);
    }

    @Override
    public String toString () {
        return metadataFormat();
    }
}