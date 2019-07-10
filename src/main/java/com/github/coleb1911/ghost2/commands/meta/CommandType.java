package com.github.coleb1911.ghost2.commands.meta;

public enum CommandType {
    OPERATOR("\u26D4"),
    CONFIG("\u2699"),
    FUN("\uD83C\uDF89"),
    INFO("\u2139"),
    MODERATION("\uD83D\uDEE1"),
    MUSIC("\uD83C\uDFB5"),
    UTILITY("\uD83D\uDD27");

    private String icon;

    CommandType(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public String getFormattedName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
