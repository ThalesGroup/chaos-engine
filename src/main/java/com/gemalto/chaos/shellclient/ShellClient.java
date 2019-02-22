package com.gemalto.chaos.shellclient;

import org.springframework.core.io.Resource;

public interface ShellClient {
    String runCommand (String command);

    String runResource (Resource resource);
}
