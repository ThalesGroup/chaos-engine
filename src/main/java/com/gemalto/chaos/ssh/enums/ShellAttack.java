package com.gemalto.chaos.ssh.enums;

public enum ShellAttack {
    FORKMBOMB("bomb() {  bomb | bomb &}; bomb");
    private String attack;

    ShellAttack (String command) {
        this.attack = command;
    }

    @Override
    public String toString () {
        return attack;
    }
}
