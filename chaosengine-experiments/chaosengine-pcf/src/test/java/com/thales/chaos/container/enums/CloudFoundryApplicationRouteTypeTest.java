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

package com.thales.chaos.container.enums;

import org.junit.Test;

import static com.thales.chaos.container.enums.CloudFoundryApplicationRouteType.*;
import static org.junit.Assert.assertEquals;

public class CloudFoundryApplicationRouteTypeTest {
    @Test
    public void testStringMapping () {
        assertEquals(HTTP, mapFromString(""));
        assertEquals(HTTP, mapFromString(null));
        assertEquals(TCP, mapFromString("tcp"));
        assertEquals(UNKNOWN, mapFromString("bogus"));
    }
}