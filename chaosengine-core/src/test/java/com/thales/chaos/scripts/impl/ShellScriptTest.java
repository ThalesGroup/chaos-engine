package com.thales.chaos.scripts.impl;

import com.thales.chaos.exception.ChaosException;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(Enclosed.class)
public class ShellScriptTest {
    @RunWith(MockitoJUnitRunner.class)
    public static class ShellScriptTestInner {
        @Mock
        private Resource resource;
        private ShellScript formattedShellScript;
        private ShellScript basicShellScript;
        private ShellScript semiBasicShellScript;
        private String filename = randomUUID().toString();
        private String shebang = "#!/bin/sh";
        private String description = randomUUID().toString();
        private String dependencies = "awk, grep, ps, kill, python, ";
        private String descriptionComment = "# Description: " + description;
        private String dependencyComment = "# Dependencies: " + dependencies;
        private String scriptBody = "ps aux | \\\nsort | \\\n kill -9";
        private String scriptContents = String.join("\n", shebang, dependencyComment, descriptionComment, scriptBody);
        private InputStream formattedSampleFile = new ByteArrayInputStream(scriptContents.getBytes());
        private InputStream basicSampleFile = new ByteArrayInputStream(scriptBody.getBytes());
        private InputStream semiBasicSampleFile = new ByteArrayInputStream(("#Starts with a comment\n" + scriptBody).getBytes());

        @Before
        public void fromResource () throws Exception {
            doReturn(filename).when(resource).getFilename();
            doReturn(formattedSampleFile, basicSampleFile, semiBasicSampleFile).when(resource).getInputStream();
            formattedShellScript = ShellScript.fromResource(resource);
            basicShellScript = ShellScript.fromResource(resource);
            semiBasicShellScript = ShellScript.fromResource(resource);
        }

        @Test(expected = ChaosException.class)
        public void fromResourceException () throws Exception {
            doThrow(new IOException()).when(resource).getInputStream();
            ShellScript.fromResource(resource);
        }

        @Test
        public void getDescription () {
            assertEquals(description, formattedShellScript.getDescription());
            assertEquals("No description provided", basicShellScript.getDescription());
        }

        @Test
        public void getDependencies () {
            String[] splitDependencies = (shebang.substring(2) + ", " + dependencies).split(", ");
            assertThat(formattedShellScript.getDependencies(), containsInAnyOrder(splitDependencies));
            assertThat(basicShellScript.getDependencies(), containsInAnyOrder("ps", "sort", "kill"));
            assertThat(semiBasicShellScript.getDependencies(), containsInAnyOrder("ps", "sort", "kill"));
        }

        @Test
        public void getCommentBlock () {
            assertThat(formattedShellScript.getCommentBlock(), containsInAnyOrder(shebang, descriptionComment, dependencyComment));
            assertThat(basicShellScript.getCommentBlock(), IsEmptyCollection.emptyCollectionOf(String.class));
            assertThat(semiBasicShellScript.getCommentBlock(), containsInAnyOrder("#Starts with a comment"));
        }

        @Test
        public void getScriptContents () {
            assertEquals(scriptContents, formattedShellScript.getScriptContents());
            assertEquals(scriptBody, basicShellScript.getScriptContents());
        }

        @Test
        public void getScriptResource () {
            assertEquals(resource, formattedShellScript.getResource());
            assertEquals(resource, basicShellScript.getResource());
        }

        @Test
        public void getScriptName () {
            assertEquals(filename, formattedShellScript.getScriptName());
            assertEquals(filename, basicShellScript.getScriptName());
        }

        @Test
        public void doesNotUseMissingDependencies () {
            assertTrue(formattedShellScript.doesNotUseMissingDependencies(Collections.singleton("sort")));
            assertTrue(basicShellScript.doesNotUseMissingDependencies(Collections.singleton("grep")));
            assertFalse(formattedShellScript.doesNotUseMissingDependencies(Collections.singleton("ps")));
            assertFalse(basicShellScript.doesNotUseMissingDependencies(Collections.singleton("kill")));
            assertTrue(formattedShellScript.doesNotUseMissingDependencies(Arrays.asList("sort", "sed")));
            assertTrue(basicShellScript.doesNotUseMissingDependencies(Arrays.asList("grep", "sed")));
            assertFalse(formattedShellScript.doesNotUseMissingDependencies(Arrays.asList("ps", "sort")));
            assertFalse(basicShellScript.doesNotUseMissingDependencies(Arrays.asList("kill", "grep")));
        }

        @Test
        public void requiresCattle () throws Exception {
            String petScriptBody = String.join("\n", "# Health check: whoami", "# Self healing: init 6", "kill -9 1");
            InputStream petInputStream = new ByteArrayInputStream(petScriptBody.getBytes());
            String cattleScriptBody = String.join("\n", "#!/bin/bash", "kill -9 1");
            InputStream cattleInputStream = new ByteArrayInputStream(cattleScriptBody.getBytes());
            doReturn(petInputStream, cattleInputStream).when(resource).getInputStream();
            ShellScript petScript = ShellScript.fromResource(resource);
            ShellScript cattleScript = ShellScript.fromResource(resource);
            assertFalse(petScript.isRequiresCattle());
            assertTrue(cattleScript.isRequiresCattle());
        }

        @Test
        public void selfHealingCommand () throws Exception {
            String basicScript = String.join("\n", "# Health check: whoami", "# Self healing: init 6", "kill -9 1");
            InputStream scriptInputStream = new ByteArrayInputStream(basicScript.getBytes());
            doReturn(scriptInputStream).when(resource).getInputStream();
            ShellScript script = ShellScript.fromResource(resource);
            assertEquals("init 6", script.getSelfHealingCommand());
        }

        @Test
        public void healthCheckCommand () throws Exception {
            String basicScript = String.join("\n", "# Health check: whoami", "# Self healing: init 6", "kill -9 1");
            InputStream scriptInputStream = new ByteArrayInputStream(basicScript.getBytes());
            doReturn(scriptInputStream).when(resource).getInputStream();
            ShellScript script = ShellScript.fromResource(resource);
            assertEquals("whoami", script.getHealthCheckCommand());
        }
    }

    @RunWith(Parameterized.class)
    public static class ShellScriptResourceTest {
        private final boolean exceptionExpected;
        private final Resource resource;
        private final boolean expectedCattle;
        private ShellScript shellScript;

        public ShellScriptResourceTest (String resourcePath) {
            this.resource = new ClassPathResource("ssh/testscripts/" + resourcePath);
            this.expectedCattle = resourcePath.startsWith("cattle");
            this.exceptionExpected = resourcePath.startsWith("invalid");
        }

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object> parameters () {
            return List.of("pets/fullHeader.sh", "pets/headerWithoutShebang.sh", "cattle/fullHeader.sh", "cattle/noHeader.sh", "cattle/headerWithoutShebang.sh", "cattle/shebangOnly.sh", "invalid/healthcheckOnly.sh", "invalid/selfhealingOnly.sh");
        }

        @Before
        public void setUp () {
            try {
                shellScript = ShellScript.fromResource(resource);
                assertFalse("An exception was " + (exceptionExpected ? "" : "not ") + " expected", exceptionExpected);
            } catch (ChaosException e) {
                assertTrue("An unexpected exception was thrown", exceptionExpected);
            }
        }

        @Test
        public void testResource () {
            if (shellScript != null) {
                assertEquals(expectedCattle, shellScript.isRequiresCattle());
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class ProvidedShellScriptValidity {
        private final Resource resource;

        public ProvidedShellScriptValidity (Resource resource) {
            this.resource = resource;
        }

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object> parameters () throws Exception {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:/ssh/experiments/*");
            return List.of(resources);
        }

        @Test
        public void buildsShellScript () {
            try {
                ShellScript.fromResource(resource);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        @Test
        public void isNotFromTestPath () throws Exception {
            assertThat("Path of main Shell Scripts was overridden in test", resource.getFile()
                                                                                    .getPath(), not(containsString("test-classes")));
        }
    }
}

