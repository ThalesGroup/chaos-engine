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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

class GcpSSHKeyMetadataTest {
    private String rawKeys = "user:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDFurPWTlzJ3U3/ZX8NJY/vjNmN8sPJ9LXGLkGsQykXWd7WR0jBkCTFSJkzCogkGtrDKSCTsqDRlOYB/rGSNxBoiCjHg/gtXgsWm6c0B+Uz581vg9HvHn/NougV5N0BGnoiJwcO7lter86pScrUuPy1nJ/zBOHgo/9S3M6ha66SG3pB6uOUhdT0QW9TlP7nug8cB3z3OYRGA2hd1xAt/TEKq9+XPs4viYnvL3LohqRaNQaJUyVzvcn46MVc4BzBdP3sBMQ0ERAUs4PxIFhdYZSdf5ThgmeiYrEF0X49OXvvhmY/Z8E5fBqyP8CLROUG6O37bg/RKiJggU1A/FY5vHhd user@VirtualBox\n" + "root:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDiuHU9hnymv/MQhcv/hOh0uXy4mItMkh42OGX6+iSCSHcHg2pEpDKIMeiiJaa4k0YlbYEO2unKiwI6W7Ts8+F91pbvKYbcbb3GVre/BtgDJvehflDAMM8QrlEBGilfTVOsmuIDpj7jgI598NV54LM/Ujz8df6vgigse2pcqaXrW0cpbaqeBlN0jMhhnSAC9pIddDzyzNyBzYCz1dYli6N3myty/DJZiTxU5NekvOXY1kxdoUbmju9smrUvpj5bFYvXpOQExde3PAqJFwGFlECPXbDuMUoYlbBFWsPAKUATYMJdEjGhOAC8XSwDgETeI67GoLUcUCfMGKz8od0oo/W9 root@virtualMachine\n";
    private List<GcpSSHKeyMetadata> keys = List.of(new GcpSSHKeyMetadata("user",
                    "AAAAB3NzaC1yc2EAAAADAQABAAABAQDFurPWTlzJ3U3/ZX8NJY/vjNmN8sPJ9LXGLkGsQykXWd7WR0jBkCTFSJkzCogkGtrDKSCTsqDRlOYB/rGSNxBoiCjHg/gtXgsWm6c0B+Uz581vg9HvHn/NougV5N0BGnoiJwcO7lter86pScrUuPy1nJ/zBOHgo/9S3M6ha66SG3pB6uOUhdT0QW9TlP7nug8cB3z3OYRGA2hd1xAt/TEKq9+XPs4viYnvL3LohqRaNQaJUyVzvcn46MVc4BzBdP3sBMQ0ERAUs4PxIFhdYZSdf5ThgmeiYrEF0X49OXvvhmY/Z8E5fBqyP8CLROUG6O37bg/RKiJggU1A/FY5vHhd",
                    "user@VirtualBox"),
            new GcpSSHKeyMetadata("root",
                    "AAAAB3NzaC1yc2EAAAADAQABAAABAQDiuHU9hnymv/MQhcv/hOh0uXy4mItMkh42OGX6+iSCSHcHg2pEpDKIMeiiJaa4k0YlbYEO2unKiwI6W7Ts8+F91pbvKYbcbb3GVre/BtgDJvehflDAMM8QrlEBGilfTVOsmuIDpj7jgI598NV54LM/Ujz8df6vgigse2pcqaXrW0cpbaqeBlN0jMhhnSAC9pIddDzyzNyBzYCz1dYli6N3myty/DJZiTxU5NekvOXY1kxdoUbmju9smrUvpj5bFYvXpOQExde3PAqJFwGFlECPXbDuMUoYlbBFWsPAKUATYMJdEjGhOAC8XSwDgETeI67GoLUcUCfMGKz8od0oo/W9",
                    "root@virtualMachine"));

    @Test
    void parseMetadata () {
        List<GcpSSHKeyMetadata> actualKeys = GcpSSHKeyMetadata.parseMetadata(rawKeys);
        assertEquals(keys, actualKeys);
    }

    @Test
    void metadataFormat () {
        String actualRaw = GcpSSHKeyMetadata.metadataFormat(keys);
        assertEquals(rawKeys, actualRaw);
    }

    @Test
    void continuousConsistency () {
        String newRaw;
        String oldRaw = rawKeys;
        for (int i = 0; i < 10; i++) {
            newRaw = GcpSSHKeyMetadata.metadataFormat(GcpSSHKeyMetadata.parseMetadata(oldRaw));
            assertEquals(newRaw, oldRaw);
            oldRaw = newRaw;
        }
    }
}