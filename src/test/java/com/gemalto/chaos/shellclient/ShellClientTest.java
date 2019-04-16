package com.gemalto.chaos.shellclient;

import com.google.common.primitives.Ints;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ShellClientTest {
    private ShellClient shellClient;
    private final String capability = randomUUID().toString();
    private boolean expected;
    private Map<String, Integer> commandAndExitCode;

    public ShellClientTest (boolean expected, Map<String, Integer> commandAndExitCode) {
        this.expected = expected;
        this.commandAndExitCode = commandAndExitCode;
    }

    @Parameterized.Parameters(name = "Expected {0}, Input combination {1}")
    public static List<Object[]> params () {
        int[][] exitCombinations = new int[][]{ { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 }, { 1, 0, 0 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 }, { -1, -1, -1 }, { 127, 127, 127 }, { 127, 0, 127 } };
        List<Object[]> parameters = new LinkedList<>();
        for (int i = 0; i < exitCombinations.length; i++) {
            boolean expected = false;
            if (Ints.contains(exitCombinations[i], 0)) {
                expected = true;
            }
            Map<String, Integer> combination = new LinkedHashMap<>();
            combination.put("command -v ", exitCombinations[i][0]);
            combination.put("which ", exitCombinations[i][1]);
            combination.put("type ", exitCombinations[i][2]);
            parameters.add(new Object[]{ expected, combination });
        }
        return parameters;
    }

    @Before
    public void setUp () throws Exception {
        shellClient = mock(ShellClient.class);
    }

    @Test
    public void checkDependency () {
        doCallRealMethod().when(shellClient).checkDependency(any());
        for (Map.Entry<String, Integer> entry : commandAndExitCode.entrySet()) {
            doReturn(ShellOutput.builder().withExitCode(entry.getValue()).build()).when(shellClient)
                                                                                  .runCommand(entry.getKey() + capability);
        }
        assertEquals(expected, shellClient.checkDependency(capability));
    }
}