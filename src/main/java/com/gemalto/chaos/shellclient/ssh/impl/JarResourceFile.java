package com.gemalto.chaos.shellclient.ssh.impl;

import com.gemalto.chaos.shellclient.ShellConstants;
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
