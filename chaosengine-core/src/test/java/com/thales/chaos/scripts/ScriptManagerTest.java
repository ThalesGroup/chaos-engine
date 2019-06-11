package com.thales.chaos.scripts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.not;
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
    public void assertNotEmpty () {
        assertThat(scriptManager.getScripts(), not(hasSize(0)));
    }

    @Test
    public void assertEmptyWithoutFilesystem () {
        // This works because during testing, the files are loaded via filesystem and not yet in a Jar
        ScriptManager newScriptManager = new ScriptManager(false);
        assertThat(newScriptManager.getScripts(), hasSize(0));
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        ScriptManager scriptManager () {
            return Mockito.spy(new ScriptManager(true));
        }
    }
}