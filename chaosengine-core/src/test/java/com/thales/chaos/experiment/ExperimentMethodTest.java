/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.experiment;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.scripts.Script;
import com.thales.chaos.scripts.impl.ShellScript;
import com.thales.chaos.shellclient.ShellOutput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ExperimentMethodTest {
    private static final String HEALTH_CHECK_COMMAND = "health check";
    private static final String SELF_HEALING_COMMAND = "self healing";
    private static final String FINALIZE_COMMAND = "finalize command";
    private final Container container = mock(Container.class);
    private ExperimentMethod<Container> experimentMethod;
    private Script script;
    private Experiment experiment = spy(new Experiment(container) {
    });

    public ExperimentMethodTest (String resourceName) {
        this.script = ShellScript.fromResource(new ClassPathResource("com/thales/chaos/experiment/experimentMethodTest/" + resourceName));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters () throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("com/thales/chaos/experiment/experimentMethodTest/*");
        return Arrays.stream(resources).map(Resource::getFilename).map(o -> new Object[]{ o }).collect(Collectors.toList());
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp () {
        doReturn(true).when(container).supportsShellBasedExperiments();
        doReturn(false).when(container).isCattle();
        doNothing().when(experiment).sendNotification(any(), any());
        experimentMethod = ExperimentMethod.fromScript(container, script);
        experimentMethod.accept(container, experiment);
    }

    @Test
    public void checkContainerHealth () throws Exception {
        assumeThat(script.getHealthCheckCommand(), is(HEALTH_CHECK_COMMAND));
        ContainerHealth containerHealth;
        doReturn(ShellOutput.builder().withExitCode(1).build()).when(container).runCommand(HEALTH_CHECK_COMMAND);
        containerHealth = experiment.getCheckContainerHealth().call();
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, containerHealth);
        doReturn(ShellOutput.builder().withExitCode(0).build()).when(container).runCommand(HEALTH_CHECK_COMMAND);
        containerHealth = experiment.getCheckContainerHealth().call();
        assertEquals(ContainerHealth.NORMAL, containerHealth);
        doThrow(new RuntimeException()).when(container).runCommand(HEALTH_CHECK_COMMAND);
        containerHealth = experiment.getCheckContainerHealth().call();
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, containerHealth);
    }

    @Test
    public void callSelfHealing () {
        assumeThat(script.getSelfHealingCommand(), is(SELF_HEALING_COMMAND));
        doReturn(true).when(experiment).canRunSelfHealing();
        doNothing().when(experiment).evaluateRunningExperiment();
        experiment.callSelfHealing();
        verify(container, times(1)).runCommand(SELF_HEALING_COMMAND);
    }

    @Test
    public void callFinalizeCommand () {
        assumeThat(script.getFinalizeCommand(), is(FINALIZE_COMMAND));
        experiment.getFinalizeMethod().run();
        verify(container, times(1)).runCommand(FINALIZE_COMMAND);
    }
}