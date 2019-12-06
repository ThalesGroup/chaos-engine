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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class GoogleCloudMetadataUtil {
    private static final URI GOOGLE_CLOUD_INSTANCE_METADATA_URI = UriComponentsBuilder.newInstance()
                                                                                      .scheme("http")
                                                                                      .host("metadata.goog")
                                                                                      .port(80)
                                                                                      .path("/computeMetadata/v1/instance/")
                                                                                      .queryParam("recursive", "true")
                                                                                      .build()
                                                                                      .toUri();
    private static GoogleCloudInstanceIdentity googleCloudInstanceIdentity;

    private GoogleCloudMetadataUtil () {
    }

    public static GoogleCloudInstanceIdentity getGoogleCloudInstanceIdentity () {
        if (googleCloudInstanceIdentity == null) fetchGoogleCloudInstanceIdentity();
        return googleCloudInstanceIdentity;
    }

    private static void fetchGoogleCloudInstanceIdentity () {
        fetchGoogleCloudInstanceIdentity(GOOGLE_CLOUD_INSTANCE_METADATA_URI);
    }

    static void fetchGoogleCloudInstanceIdentity (URI endpoint) {
        RestTemplate restTemplate;
        restTemplate = new RestTemplate();
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Metadata-Flavor", "Google");
            HttpEntity<?> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<GoogleCloudInstanceIdentity> response = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, GoogleCloudInstanceIdentity.class);
            googleCloudInstanceIdentity = response.getBody();
        } catch (RuntimeException ignored) {
            /*
            ResourceAccessException will occur if RestTemplate cannot reach the endpoint. We'll just let the identity stay null.
            */
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleCloudInstanceIdentity {
        @JsonProperty
        private Long id;
        @JsonProperty
        private String image;
        @JsonProperty
        private String name;
        @JsonProperty
        private String zone;

        public Long getId () {
            return id;
        }

        public String getImage () {
            return image;
        }

        public String getName () {
            return name;
        }

        public String getZone () {
            return zone;
        }
    }
}
