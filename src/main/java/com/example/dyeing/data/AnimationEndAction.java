package com.example.dyeing.data;

public enum AnimationEndAction {
    START("start"),
    END("end"),
    REMOVE("remove");

    private final String commandName;

    AnimationEndAction(String commandName) {
        this.commandName = commandName;
    }

    public String commandName() {
        return this.commandName;
    }

    public static AnimationEndAction fromCommandName(String name) {
        for (AnimationEndAction value : values()) {
            if (value.commandName.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown end action: " + name);
    }
}
