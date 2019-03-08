package com.gemalto.chaos.shellclient;

import org.apache.logging.log4j.util.Strings;

public class ShellOutput {
    public static final ShellOutput EMPTY_SHELL_OUTPUT = new ShellOutput(-1, Strings.EMPTY, Strings.EMPTY);
    private int exitCode;
    private String stdOut;
    private String stdErr;

    public ShellOutput (int exitCode, String stdOut, String stdErr) {
        this(exitCode, stdOut);
        this.stdErr = stdErr;
    }

    public ShellOutput (int exitCode, String stdOut) {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
    }

    public static ShellOutputBuilder builder () {
        return ShellOutputBuilder.aShellOutput();
    }

    public int getExitCode () {
        return exitCode;
    }

    public void setExitCode (int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStdOut () {
        return stdOut;
    }

    public void setStdOut (String stdOut) {
        this.stdOut = stdOut;
    }

    public String getStdErr () {
        return stdErr;
    }

    public void setStdErr (String stdErr) {
        this.stdErr = stdErr;
    }

    public static final class ShellOutputBuilder {
        private int exitCode;
        private String stdOut;
        private String stdErr;

        private ShellOutputBuilder () {
        }

        public static ShellOutputBuilder aShellOutput () {
            return new ShellOutputBuilder();
        }

        public ShellOutputBuilder withExitCode (int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public ShellOutputBuilder withStdOut (String stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        public ShellOutputBuilder withStdErr (String stdErr) {
            this.stdErr = stdErr;
            return this;
        }

        public ShellOutput build () {
            ShellOutput shellOutput = new ShellOutput(exitCode, stdOut);
            shellOutput.setStdErr(stdErr);
            return shellOutput;
        }
    }
}
