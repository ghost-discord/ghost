package com.github.coleb1911.ghost2.commands.meta;

public enum CommandType {
    OPERATOR,
    CONFIGURATION,
    FUN,
    INFORMATION,
    MODERATION,
    MUSIC,
    UTILITY;

    public String getFormattedName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
