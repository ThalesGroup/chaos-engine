package com.gemalto.chaos.shellclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class ShellClientTest {
    @Mock
    private ShellClient shellClient;

    @Test
    public void checkDependency () {
        doCallRealMethod().when(shellClient).checkDependency(any());
        doReturn(ShellOutput.builder().withExitCode(0).build()).when(shellClient).runCommand("which success");
        doReturn(ShellOutput.builder().withExitCode(1).build()).when(shellClient).runCommand("which fail");
        assertTrue(shellClient.checkDependency("success"));
        assertFalse(shellClient.checkDependency("fail"));
    }
}