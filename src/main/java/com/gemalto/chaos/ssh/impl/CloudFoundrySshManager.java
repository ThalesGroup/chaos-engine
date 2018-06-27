package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfo;
import com.gemalto.chaos.ssh.SshManager;
import net.schmizz.sshj.connection.channel.direct.Session;

public class CloudFoundrySshManager extends SshManager {
    private CloudFoundryPlatformInfo cloudFoundryPlatformInfo;

    public CloudFoundrySshManager (CloudFoundryPlatformInfo cloudFoundryPlatformInfo) {
        super(cloudFoundryPlatformInfo.getApplicationSshEndpoint(), cloudFoundryPlatformInfo.getApplicationSshPort());
        this.cloudFoundryPlatformInfo = cloudFoundryPlatformInfo;
    }

    public Session connect (CloudFoundryContainer container) {
        String cfContainerUsername = "cf:" + container.getApplicationId() + "/" + container.getInstance();
        return super.connect(cfContainerUsername, cloudFoundryPlatformInfo.getSshCode());
    }
}
