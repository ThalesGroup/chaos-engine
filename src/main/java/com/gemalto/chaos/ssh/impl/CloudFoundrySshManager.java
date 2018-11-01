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
    private CloudFoundryContainer container;

    public CloudFoundrySshManager (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryPlatformInfo.getApplicationSshEndpoint(), cloudFoundryPlatformInfo.getApplicationSshPort());
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    @Override
    public void executeCommandInInteractiveShell (String command, String shellName, int maxSessionDuration) throws IOException {
        super.executeCommandInInteractiveShell(command, shellName + " : " + container.getName(), maxSessionDuration);
    }

    //https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html
    public boolean connect (CloudFoundryContainer container) throws IOException {
        log.debug("Establishing ssh connection to app container {}, instance {}", container.getName(), container.getInstance());
        String cfContainerUsername = "cf:" + container.getApplicationId() + "/" + container.getInstance();
        this.container = container;
        return super.connect(cfContainerUsername, cloudFoundryPlatformInfo.getSshCode());
    }
}
