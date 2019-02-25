package com.gemalto.chaos.shellclient;

import org.springframework.core.io.Resource;

import java.io.Closeable;

public interface ShellClient extends Closeable {
    String runCommand (String command);

    String runResource (Resource resource);
}
