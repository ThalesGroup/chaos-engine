/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

package com.thales.chaos.shellclient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ShellClientTest {
    private static final String capability = UUID.randomUUID().toString();
    private static final List<String> commandsInOrder = List.of("command -v", "which", "type");
    private static final List<Integer> exitCodes = List.of(0, 1, 127);
    private final SortedMap<String, Integer> commandAndExitCode;
    private ShellClient shellClient;
    private AtomicInteger expected;
    private Map<String, Integer> expectedCalls = new HashMap<>();

    public ShellClientTest (SortedMap<String, Integer> commandAndExitCode) {
        this.commandAndExitCode = commandAndExitCode;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> params () {
        List<Object[]> params = new ArrayList<>();
        /*
        I would love to somehow do this in a recursive way, but I can't think of how
        to right now. Enjoy the nested For loops!
         */
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    params.add(new Object[]{ buildCommandAndExitCode(List.of(exitCodes.get(i), exitCodes.get(j), exitCodes
                            .get(k))) });
                }
            }
        }
        return params;
    }

    private static SortedMap<String, Integer> buildCommandAndExitCode (List<Integer> exitCodes) {
        assertEquals(commandsInOrder.size(), exitCodes.size());
        final SortedMap<String, Integer> map = new TreeMap<>(Comparator.comparingInt(commandsInOrder::indexOf));
        for (int i = 0; i < commandsInOrder.size(); i++) {
            map.put(commandsInOrder.get(i), exitCodes.get(i));
        }
        return map;
    }

    @Before
    public void setUp () {
        shellClient = mock(ShellClient.class);
        doCallRealMethod().when(shellClient).checkDependency(any());
        expected = new AtomicInteger(1);
        commandAndExitCode.entrySet()
                          .stream()
                          .peek(stringIntegerEntry -> doReturn(ShellOutput.builder()
                                                                          .withExitCode(stringIntegerEntry.getValue())
                                                                          .build()).when(shellClient).runCommand(stringIntegerEntry.getKey() + " " + capability))
                          .forEach(stringIntegerEntry -> {
                              expectedCalls.put(stringIntegerEntry.getKey(), expected.get());
                              if (expected.get() == 1 && stringIntegerEntry.getValue() == 0) {
                                  expected.set(0);
                              }
                          });
    }

    @Test
    public void checkDependency () {
        assertEquals(expected.get() == 0, shellClient.checkDependency(capability));
        expectedCalls.forEach((s, i) -> verify(shellClient, times(i)).runCommand(s + " " + capability));
    }
}