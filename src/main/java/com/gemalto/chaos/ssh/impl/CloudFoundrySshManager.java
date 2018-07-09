package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfo;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfoFactory;
import com.gemalto.chaos.ssh.SshManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(CloudFoundryPlatformInfoFactory.class)
public class CloudFoundrySshManager extends SshManager {
    @Autowired
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;
    private CloudFoundryContainer container;

    public CloudFoundrySshManager (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryPlatformInfo.getApplicationSshEndpoint(), cloudFoundryPlatformInfo.getApplicationSshPort());
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    @Override
    public void executeCommandInInteractiveShell (String command, String shellName, int maxSessionDuration) {
        super.executeCommandInInteractiveShell(command, shellName + " : " + container.getName(), maxSessionDuration);
    }

    //https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html
    public boolean connect (CloudFoundryContainer container) {
        String cfContainerUsername = "cf:" + container.getApplicationId() + "/" + container.getInstance();
        this.container = container;
        return super.connect(cfContainerUsername, cloudFoundryPlatformInfo.getSshCode());
    }
}
