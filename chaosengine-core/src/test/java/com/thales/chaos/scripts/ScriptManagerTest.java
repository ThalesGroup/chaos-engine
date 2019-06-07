package com.thales.chaos.scripts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ScriptManagerTest {
    @Autowired
    private ScriptManager scriptManager;

    @Test
    public void assertLoaded () {
        verify(scriptManager, times(1)).populateScriptsFromResources();
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        ScriptManager scriptManager () {
            return Mockito.spy(new ScriptManager());
        }
    }
}