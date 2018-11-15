package com.gemalto.chaos.ssh.services;

import com.gemalto.chaos.ssh.impl.experiments.ForkBomb;
import com.gemalto.chaos.ssh.impl.experiments.RandomProcessTermination;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ShResourceServiceTest {
    ShResourceService service ;

    @Before
    public void setUp () throws Exception {
        service = new ShResourceService();
    }

    @Test
    public void verifyScriptPresents(){
        assertTrue(service.getScriptResource(ForkBomb.EXPERIMENT_SCRIPT).exists());
        assertTrue(service.getScriptResource(RandomProcessTermination.EXPERIMENT_SCRIPT).exists());
    }
}