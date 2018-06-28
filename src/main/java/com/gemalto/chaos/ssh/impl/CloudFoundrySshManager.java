package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfo;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfoFactory;
import com.gemalto.chaos.ssh.SshManager;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(CloudFoundryPlatformInfoFactory.class)
public class CloudFoundrySshManager extends SshManager {
    @Autowired
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
