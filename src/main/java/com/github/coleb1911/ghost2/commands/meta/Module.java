package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.object.reaction.ReactionEmoji;

import javax.validation.constraints.NotNull;

/**
 * A single command module.
 * <p>
 * To implement a command, simply extend {@code Module} and provide at least the following:
 * <ul>
 * <li>A {@code public} constructor</li>
 * <li>{@linkplain ModuleInfo.Builder#withName Command name}</li>
 * <li>{@linkplain ModuleInfo.Builder#withDescription Command description}</li>
 * <li>{@link Module#invoke invoke} implementation</li>
 * </ul>
 * {@link com.github.coleb1911.ghost2.commands.modules.utility.ModulePing ModulePing} provides a good bare-minimum example of a working Module.
 */
public abstract class Module {
    public static final String REPLY_INSUFFICIENT_PERMISSIONS_USER = "You don't have permission to run that command.";
    public static final String REPLY_INSUFFICIENT_PERMISSIONS_BOT = "I don't have sufficient permissions to run that command.";
    public static final String REPLY_COMMAND_INVALID = "That command doesn't exist. See `help` for a list of valid commands and their arguments.";
    public static final String REPLY_ARGUMENT_INVALID = "Invalid argument. See `help` for a list of valid commands and their arguments.";
    public static final String REPLY_GENERAL_ERROR = "Whoops! An error occurred somewhere along the line. My operator has been notified.";

    public static final ReactionEmoji REACT_OK = ReactionEmoji.unicode("\u2705");
    public static final ReactionEmoji REACT_DENIED = ReactionEmoji.unicode("\u26D4");
    public static final ReactionEmoji REACT_WARNING = ReactionEmoji.unicode("\u26A0");

    private final ModuleInfo info;

    /**
     * Constructs a new Module. Should only be used by subclasses of Module.
     *
     * @param info ModuleInfo.Builder with the subclass' metadata
     */
    @ReflectiveAccess
    protected Module(ModuleInfo.Builder info) {
        this.info = info.build();
    }

    /**
     * @return The Module info
     * @see ModuleInfo
     */
    public ModuleInfo getInfo() {
        return info;
    }

    /**
     * Invokes the command.
     *
     * @param ctx Command context. <b>Cannot be null.</b>
     */
    @ReflectiveAccess
    public abstract void invoke(@NotNull final CommandContext ctx);

    @Override
    public boolean equals(Object other) {
        return other instanceof Module && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return info.getName().hashCode();
    }
}