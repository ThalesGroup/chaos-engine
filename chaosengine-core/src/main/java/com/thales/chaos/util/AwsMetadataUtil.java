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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

public class AwsMetadataUtil {
    private AwsMetadataUtil () {
    }

    private static final URI AWS_DYNAMIC_INSTANCE_IDENTITY_DOCUMENT_URI = UriComponentsBuilder.newInstance()
                                                                                              .scheme("http")
                                                                                              .host("169.254.169.254")
                                                                                              .port(80)
                                                                                              .path("/latest/dynamic/instance-identity/document")
                                                                                              .build()
                                                                                              .toUri();
    private static AwsInstanceIdentity awsInstanceIdentity;

    public static AwsInstanceIdentity getAwsInstanceIdentity () {
        if (awsInstanceIdentity == null) fetchAwsInstanceIdentity();
        return awsInstanceIdentity;
    }

    private static void fetchAwsInstanceIdentity () {
        fetchAwsInstanceIdentity(AWS_DYNAMIC_INSTANCE_IDENTITY_DOCUMENT_URI);
    }

    static void fetchAwsInstanceIdentity (URI endpoint) {
        RestTemplate restTemplate;
        MappingJackson2HttpMessageConverter mappingConverter = new MappingJackson2HttpMessageConverter();
        mappingConverter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));
        restTemplate = new RestTemplate(List.of(mappingConverter));
        try {
            ResponseEntity<AwsInstanceIdentity> response = restTemplate.getForEntity(endpoint, AwsInstanceIdentity.class);
            awsInstanceIdentity = response.getBody();
        } catch (ResourceAccessException ignored) {
            /*
            ResourceAccessException will occur if RestTemplate cannot reach the endpoint. We'll just let the identity stay null.
            */
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AwsInstanceIdentity {
        @JsonProperty
        private String accountId;
        @JsonProperty
        private String availabilityZone;
        @JsonProperty
        private String region;
        @JsonProperty
        private String imageId;
        @JsonProperty
        private String instanceId;

        public String getAccountId () {
            return accountId;
        }

        public String getAvailabilityZone () {
            return availabilityZone;
        }

        public String getRegion () {
            return region;
        }

        public String getImageId () {
            return imageId;
        }

        public String getInstanceId () {
            return instanceId;
        }
    }
}
