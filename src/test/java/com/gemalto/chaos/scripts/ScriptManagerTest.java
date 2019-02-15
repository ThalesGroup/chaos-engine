package com.gemalto.chaos.scripts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
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

    @Test
    public void getScripts () {

        /*
        The size is dependent on the number of files in the test/resources/ssh/experiments folder.
        Adding more scripts for testing may require this size to be adjusted.
         */
        assertThat(scriptManager.getScripts(), hasSize(3));
    }

    @Test
    public void getScriptsWithPredicate () {
        final AtomicInteger i = new AtomicInteger(0);
        assertThat(scriptManager.getScripts(script -> i.getAndIncrement() < 1), hasSize(1));
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        ScriptManager scriptManager () {
            return Mockito.spy(new ScriptManager());
        }
    }
}