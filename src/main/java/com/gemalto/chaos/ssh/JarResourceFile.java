package com.gemalto.chaos.ssh;

import net.schmizz.sshj.xfer.FileSystemFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.*;

public class JarResourceFile extends FileSystemFile {
    private static final Logger log = LoggerFactory.getLogger(JarResourceFile.class);
    Resource resource;
    public JarResourceFile (Resource resource) {
        super(resource.getFilename());
        this.resource=resource;
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
    public InputStream getInputStream()
            throws IOException {
        return resource.getInputStream();
    }

    @Override
    public long getLastAccessTime()
            throws IOException {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public long getLastModifiedTime()
            throws IOException {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public boolean isFile () {
        return true;
    }
}
