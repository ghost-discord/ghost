package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.object.util.PermissionSet;

public abstract class Command {
    public static final String REPLY_INSUFFICIENT_PERMISSIONS = "You don't have permission to run that command.";

    private String name;
    private CommandType type;
    private String[] aliases;

    protected Command(String name, CommandType type, String... aliases) {
        this.name = name;
        this.type = type;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public CommandType getType() {
        return type;
    }

    public String[] getAliases() {
        return aliases;
    }

    public abstract void invoke(CommandContext ctx);

    public abstract PermissionSet getRequiredPermissions();

    @Override
    public boolean equals(Object other) {
        return other instanceof Command && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}