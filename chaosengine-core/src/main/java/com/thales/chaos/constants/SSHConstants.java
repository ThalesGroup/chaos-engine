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

package com.thales.chaos.constants;

import java.math.BigInteger;

public class SSHConstants {
    public static final int DEFAULT_SSH_PORT = 22;
    public static final String TEMP_DIRECTORY = "/tmp/";
    public static final BigInteger DEFAULT_RSA_PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    public static final int THIRTY_SECONDS_IN_MILLIS = 1000 * 30;
    public static final String SCRIPT_NOHUP_WRAPPER = "nohup %s > /dev/null 2>&1 < /dev/null &";

    private SSHConstants () {
    }
}
