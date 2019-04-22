package com.gemalto.chaos.shellclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ShellOutputTest {
    private final String expected;
    private final ShellOutput shellOutput;

    public ShellOutputTest (int exitCode, String stdOut, String stdErr, String expected) {
        this.expected = expected;
        this.shellOutput = new ShellOutput(exitCode, stdOut, stdErr);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters () {
        return List.of(new Object[]{ 0, "output", "error", "output" },
                       new Object[]{ 1, "output", "error", "1: error" },
                       new Object[]{ 1, "output", "", "1: output" },
                       new Object[]{ 1, "output", " ", "1: output" },
                       new Object[]{ 1, "output", null, "1: output" },
                       new Object[]{ 0, "x".repeat(200), "", "x".repeat(125) + "..." },
                       new Object[]{ 1, "x".repeat(200), "", "1: " + "x".repeat(122) + "..." },
                       new Object[]{ 1, "x".repeat(200), " ", "1: " + "x".repeat(122) + "..." },
                       new Object[]{ 1, "x".repeat(200), null, "1: " + "x".repeat(122) + "..." },
                       new Object[]{ 1, "", "e".repeat(200), "1: " + "e".repeat(122) + "..." });
    }

    @Test
    public void toStringTest () {
        assertEquals(expected, shellOutput.toString());
    }
}