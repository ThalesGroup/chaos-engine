package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfo;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfoFactory;
import com.gemalto.chaos.ssh.SshManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnBean(CloudFoundryPlatformInfoFactory.class)
public class CloudFoundrySshManager extends SshManager {
    private static final Logger log = LoggerFactory.getLogger(CloudFoundrySshManager.class);
    @Autowired
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    protected CloudFoundryContainer container;

    public CloudFoundrySshManager (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryPlatformInfo.getApplicationSshEndpoint(), cloudFoundryPlatformInfo.getApplicationSshPort());
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    @Override
    public void executeCommandInShell (String command, String shellName) throws IOException {
         super.executeCommandInShell(command, shellName + " : " + container.getName());
    }

    //https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html
    protected String composeUserName(){
        return "cf:" + container.getApplicationId() + "/" + container.getInstance();
    }
    public boolean connect (CloudFoundryContainer container) throws IOException {
        this.container = container;
        log.debug("Establishing ssh connection to app container {}, instance {}", container.getName(), container.getInstance());
        return super.connect(composeUserName(), cloudFoundryPlatformInfo.getSshCode());
    }
}
