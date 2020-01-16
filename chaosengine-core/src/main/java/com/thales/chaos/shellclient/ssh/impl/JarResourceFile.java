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

package com.thales.chaos.shellclient.ssh.impl;

import com.thales.chaos.shellclient.ShellConstants;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

public class JarResourceFile extends FileSystemFile {
    private Resource resource;
    private boolean executable;

    JarResourceFile (Resource resource, boolean isExecutable) {
        super(resource.getFilename());
        this.resource=resource;
        this.executable=isExecutable;
    }

    @Override
    public boolean isFile () {
        return true;
    }

    @Override
    public long getLength() {
        try {
            return resource.contentLength();
        } catch (IOException e) {
            log.error("Cannot evaluate resource file {} content length",resource.getFilename(),e);
        }
        return 0;
    }

    @Override
    public InputStream getInputStream () throws IOException {
        return resource.getInputStream();
    }

    @Override
    public long getLastAccessTime () {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public long getLastModifiedTime () {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public int getPermissions () {
        if (executable) {
            return ShellConstants.CHMOD_744;
        }
        return ShellConstants.CHMOD_644;
    }
}
