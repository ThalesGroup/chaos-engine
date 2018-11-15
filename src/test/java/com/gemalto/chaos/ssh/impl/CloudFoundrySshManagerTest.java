package com.gemalto.chaos.ssh.impl;

import com.gemalto.chaos.container.impl.CloudFoundryContainer;
import com.gemalto.chaos.platform.impl.CloudFoundryPlatformInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundrySshManagerTest {
    @Mock
    private CloudFoundryPlatformInfo info;
    @Mock
    private CloudFoundryContainer container ;
    private CloudFoundrySshManager sshManager;

    private static final String host= UUID.randomUUID().toString();
    private static final String sshCode= UUID.randomUUID().toString();
    private static final String applicationID= UUID.randomUUID().toString();
    private static final int instance= 0;
    private static final String port="22";

    private static final String cfContainerUsername = "cf:" + applicationID + "/" + instance;

    @Before
    public void setUp () throws Exception {
        when(info.getApplicationSshEndpoint()).thenReturn(host);
        when(info.getApplicationSshPort()).thenReturn(port);
        container= CloudFoundryContainer.builder()
                                        .applicationId(applicationID)
                                        .instance(instance)
                                        .build();

        sshManager= Mockito.spy(new CloudFoundrySshManager(info));
    }


    @Test
    public void composeUserName(){
        sshManager.container=container;
        assertEquals("cf:"+applicationID+"/" + instance,sshManager.composeUserName());
    }
    @Test
    public void endpointSetUp() {
       assertEquals(host,sshManager.getHostname());
       assertEquals(port,sshManager.getPort());
    }
}