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

package com.thales.chaos.shellclient;

import java.util.List;

public class ShellConstants {
    public static final int EXEC_BIT = 64;
    public static final int EOT_CHARACTER = 0x04;
    public static final int CHMOD_744 = 484; // 0744 in Octal
    public static final int CHMOD_644 = 420; // 0644 in Octal
    public static final String PARAMETER_DELIMITER = " ";
    public static final List<String> DEPENDENCY_TEST_COMMANDS = List.of("command -v", "which", "type");
    public static final String SUDO = "sudo";
    public static final String ROOT = "root";

    private ShellConstants () {
    }
}
