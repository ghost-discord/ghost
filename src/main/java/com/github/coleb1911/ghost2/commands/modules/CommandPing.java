package com.github.coleb1911.ghost2.commands.modules;

import com.github.coleb1911.ghost2.commands.meta.Command;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import discord4j.core.object.util.PermissionSet;

public class CommandPing extends Command {
    public CommandPing() {
        super("ping", CommandType.UTILITY);
    }

    @Override
    public void invoke(CommandContext ctx) {
        ctx.reply("Pong!");
    }

    @Override
    public PermissionSet getRequiredPermissions() {
        return PermissionSet.none();
    }
}
