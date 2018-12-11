package com.gemalto.chaos.ssh;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.ssh.enums.ShellCapabilityType;
import com.gemalto.chaos.ssh.enums.ShellCommand;
import com.gemalto.chaos.ssh.enums.ShellSessionCapabilityOption;
import com.gemalto.chaos.ssh.services.ShResourceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class SshExperimentTest {
    @Mock
    private SshManager sshManager;
    @Mock
    private SshCommandResult result;
    @Mock
    private SshCommandResult resultTypeCapability;
    @Mock
    private Resource resource;
    @Mock
    private ShResourceService shResourceService;

    class GenericSshExperiment extends SshExperiment{
        public GenericSshExperiment (String experimentName, String experimentScript) {
            super(experimentName, experimentScript);
        }

        @Override
        protected void buildRequiredCapabilities () {
        }
    }
    private static final ShellSessionCapability mandatoryCapShell= new ShellSessionCapability(ShellCapabilityType.SHELL).addCapabilityOption(ShellSessionCapabilityOption.BASH)
                                                                                                   .addCapabilityOption(ShellSessionCapabilityOption.ASH)
                                                                                                   .addCapabilityOption(ShellSessionCapabilityOption.SH);
    private static final ShellSessionCapability mandatoryCapBinaries= new ShellSessionCapability(ShellCapabilityType.BINARY).addCapabilityOption(ShellSessionCapabilityOption.TYPE);

    private static final String experimentName = "Generic SSH experiment";
    private static final String experimentScript = UUID.randomUUID().toString();
    private GenericSshExperiment genericSshExperiment = new GenericSshExperiment(experimentName,experimentScript);
    @Before
    public void setUp () throws Exception {
        when(shResourceService.getScriptResource(experimentScript)).thenReturn(resource);
        genericSshExperiment.setSshManager(sshManager).setShResourceService(shResourceService);
        when(resultTypeCapability.getExitStatus()).thenReturn(0);
        when(resultTypeCapability.getCommandOutput()).thenReturn("type");
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString() + ShellSessionCapabilityOption.TYPE)).thenReturn(resultTypeCapability);
    }

    @Test
    public void canExperiment() throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        when(result.getCommandOutput()).thenReturn("ash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        when(result.getCommandOutput()).thenReturn("sh");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        genericSshExperiment=new GenericSshExperiment(experimentName,experimentScript);
        genericSshExperiment.setSshManager(sshManager).setShResourceService(shResourceService);
        when(result.getCommandOutput()).thenReturn("BASH");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
    }

    @Test
    public void parseBinaryName () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("/bin/bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
    }

    @Test(expected = ChaosException.class)
    public void cannotExperiment () throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("uknown");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
    }

    @Test
    public void checkMandatoryShellSessionCapabilities(){
        assertEquals(2,genericSshExperiment.requiredCapabilities.size());
        assertTrue(genericSshExperiment.requiredCapabilities.contains(mandatoryCapShell));
        assertTrue(genericSshExperiment.requiredCapabilities.contains(mandatoryCapBinaries));
    }

    @Test
    public void getDetectedShellSessionCapabilities() throws IOException {
        ShellSessionCapability mandatoryCapShell= new ShellSessionCapability(ShellCapabilityType.SHELL)
                .addCapabilityOption(ShellSessionCapabilityOption.BASH);
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();
        List<ShellSessionCapability> detected = genericSshExperiment.getDetectedShellSessionCapabilities();
        assertEquals(2,detected.size());

        assertTrue(detected.contains(mandatoryCapShell));
        assertTrue(detected.contains(mandatoryCapBinaries));
    }

    @Test
    public void updateAvailableCapabilities() throws IOException {
        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("bash");
        when(sshManager.executeCommand(ShellCommand.SHELLTYPE.toString())).thenReturn(result);
        genericSshExperiment.runExperiment();

        ShellSessionCapability additionalCapability= new ShellSessionCapability(ShellCapabilityType.BINARY)
                .addCapabilityOption(ShellSessionCapabilityOption.SORT);
        genericSshExperiment.requiredCapabilities.add(additionalCapability);

        when(result.getExitStatus()).thenReturn(0);
        when(result.getCommandOutput()).thenReturn("sort");
        when(sshManager.executeCommand(ShellCommand.BINARYEXISTS.toString()+ShellSessionCapabilityOption.SORT)).thenReturn(result);

        genericSshExperiment.runExperiment();

        List<ShellSessionCapability> expectedCapabilities = new ArrayList<>();
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.SHELL)
                .addCapabilityOption(ShellSessionCapabilityOption.BASH));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY)
                .addCapabilityOption(ShellSessionCapabilityOption.TYPE));
        expectedCapabilities.add(new ShellSessionCapability(ShellCapabilityType.BINARY)
                .addCapabilityOption(ShellSessionCapabilityOption.SORT));

        List<ShellSessionCapability> capabilities =genericSshExperiment.getDetectedShellSessionCapabilities();
        assertEquals(expectedCapabilities,capabilities);
    }

    @Test
    public void setDetectedShellSessionCapabilities(){
        ShellSessionCapability mandatoryCapShell= new ShellSessionCapability(ShellCapabilityType.SHELL)
                .addCapabilityOption(ShellSessionCapabilityOption.BASH);
        GenericSshExperiment genericSshExperiment = new GenericSshExperiment(experimentName,experimentScript);
        List<ShellSessionCapability>  capabilities = new ArrayList<>();
        capabilities.add(mandatoryCapShell);
        genericSshExperiment.setDetectedShellSessionCapabilities(capabilities);
        assertEquals(capabilities,genericSshExperiment.getDetectedShellSessionCapabilities());
    }
}