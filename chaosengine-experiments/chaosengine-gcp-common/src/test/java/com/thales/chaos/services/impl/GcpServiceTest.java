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

package com.thales.chaos.services.impl;

import com.google.api.services.compute.ComputeScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class GcpServiceTest {
    @Spy
    private GcpService gcpService;
    @Mock
    private GoogleCredentials credentials;

    @Before
    public void beforeEach () {
        String iMadeThisJustForTheTest = "-----BEGIN PRIVATE KEY-----\\n" + "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAM0p9N+ur3fH3I1w\\n" + "eBsvxQ4tpVOUs89GDSMzXjiZzSG4MFTOdh8Ilf+Rh5ILAZwHulOnjytdsQvoYudJ\\n" + "Ist0lHB5MZi3gVcvO85z8XHImhQ6N9Mq4wDBsKQ5ZnEH5SCJ0huKUxZIdnoN71VT\\n" + "sbn4uR7XOveMwc6dvQPTzojF1y89AgMBAAECgYAaWxB1B7jM12Ty2objXzCeUKjT\\n" + "Yt/yeZpIclnhRYi/kyzKSDeOZwj16tkSns1XIPsDshvHQ2LyF6lU8uVAY7qJuWGZ\\n" + "y/NNJMlwSNg2dbkWbB96nLAClKEuKMaRrRwZ7NQJu8UNJuWqMiEdw2w9cOKo+N2l\\n" + "Dh5b2+2aJGsR1sVnoQJBAPF5vxcq1Cr44Gdo7TDkvd07XOMrOr0iYda8WjkpSN3k\\n" + "VpdF9zF/pcqC3sFokxcIaB4mIHlufht6FJN+bWeytiUCQQDZgRS4uMFZYm4Gb8mW\\n" + "x4wGodNaJ//FvcMZ8CoYen7x5bmFpuTkOENZEHQfWHs6WvBUO07TuyBGIcnRry7d\\n" + "Hc05AkBOp4xh3HZ9hNF7fYb9DRT3EdYAvN0GLEPYMUPmKJduh2jZH1YUTnLPUu+B\\n" + "6jE3KGrZnyumwiHbi1lWbbDvPRvtAkEAqboNtnwN/TdPDlzLXYrH3SEIsbDG6cLD\\n" + "7Yi5ALMOPqt6Uy5CLUkuXOD3DMLaHlZ6dfB1+clrTO7u816V3lx0GQJBALb76VX0\\n" + "k1iqsfrcAypf1X9pb48H1GOr2wLRmEGCkS2jujb31qMSVhIraUH1S/PSNfIvumXq\\n" + "btRZTQAuX8Aw9bU=\\n" + "-----END PRIVATE KEY-----";
        gcpService.setJsonKey("{  \"type\": \"service_account\",\n" + "  \"project_id\": \"project_id\",\n" + "  \"private_key_id\": \"private_key_id\",\n" + "  \"private_key\": \"" + iMadeThisJustForTheTest + "\",\n" + "  \"client_email\": \"chaos-engine-test@project_id.iam.gserviceaccount.com\",\n" + "  \"client_id\": \"100000000000000000000\",\n" + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" + "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/chaos-engine-test%40project_id.iam.gserviceaccount.com\"\n" + "}\n");
    }

    @Test
    public void googleCredentialsTest () throws Exception {
        ServiceAccountCredentials googleCredentials = (ServiceAccountCredentials) gcpService.googleCredentials();
        assertFalse(googleCredentials.createScopedRequired());
        assertThat(googleCredentials.getScopes(), containsInAnyOrder(ComputeScopes.CLOUD_PLATFORM));
    }

}