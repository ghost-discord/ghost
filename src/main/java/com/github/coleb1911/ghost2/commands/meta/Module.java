package com.github.coleb1911.ghost2.commands.meta;

/**
 * A single command module.
 * <p/>
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
    public static final String REPLY_COMMAND_INVALID = "That command doesn't exist. See \'g!help\' for a list of valid commands and their arguments.";
    public static final String REPLY_ARGUMENT_INVALID = "Invalid argument. See \'g!help\' for a list of valid commands and their arguments.";
    public static final String REPLY_GENERAL_ERROR = "Whoops! An error occurred somewhere along the line. My operator has been notified.";

    private final ModuleInfo info;

    protected Module(ModuleInfo.Builder info) {
        this.info = info.build();
    }

    public ModuleInfo getInfo() {
        return info;
    }

    public abstract void invoke(CommandContext ctx);

    @Override
    public boolean equals(Object other) {
        return other instanceof Module && hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return info.getName().hashCode();
    }
}