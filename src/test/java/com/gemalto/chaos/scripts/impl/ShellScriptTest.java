package com.gemalto.chaos.scripts.impl;

import com.gemalto.chaos.ChaosException;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class ShellScriptTest {
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
        assertEquals(resource, formattedShellScript.getScriptResource());
        assertEquals(resource, basicShellScript.getScriptResource());
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
        String cattleScriptBody = "# Cattle: true \nwhoami";
        String nonCattleScriptBody = "# Cattle: this isn't a statement! \n whoami";
        doReturn(new ByteArrayInputStream(cattleScriptBody.getBytes()), new ByteArrayInputStream(nonCattleScriptBody.getBytes()))
                .when(resource)
                .getInputStream();
        ShellScript cattleScript = ShellScript.fromResource(resource);
        ShellScript nonCattleScript = ShellScript.fromResource(resource);
        assertTrue(cattleScript.isRequiresCattle());
        assertFalse(nonCattleScript.isRequiresCattle());
    }

    @Test
    public void selfHealingCommand () throws Exception {
        String complexScriptBody = "# Self healing: whoami\necho 'hello, world'";
        String complexCattleScriptBody = "# Cattle: true\n" + complexScriptBody;
        doReturn(new ByteArrayInputStream(complexScriptBody.getBytes()), new ByteArrayInputStream(complexCattleScriptBody
                .getBytes())).when(resource).getInputStream();
        ShellScript complexScript = ShellScript.fromResource(resource);
        ShellScript complexCattleScript = ShellScript.fromResource(resource);
        assertEquals("whoami", complexScript.getSelfHealingCommand());
        assertNull(complexCattleScript.getSelfHealingCommand());
        assertNull(basicShellScript.getSelfHealingCommand());
        assertNull(formattedShellScript.getSelfHealingCommand());
    }

    @Test
    public void healthCheckCommand () throws Exception {
        String complexScriptBody = "# Health check: whoami\necho 'hello, world'";
        String complexCattleScriptBody = "# Cattle: true\n" + complexScriptBody;
        doReturn(new ByteArrayInputStream(complexScriptBody.getBytes()), new ByteArrayInputStream(complexCattleScriptBody
                .getBytes())).when(resource).getInputStream();
        ShellScript complexScript = ShellScript.fromResource(resource);
        ShellScript complexCattleScript = ShellScript.fromResource(resource);
        assertEquals("whoami", complexScript.getHealthCheckCommand());
        assertNull(complexCattleScript.getHealthCheckCommand());
        assertNull(basicShellScript.getHealthCheckCommand());
        assertNull(formattedShellScript.getHealthCheckCommand());
    }


}