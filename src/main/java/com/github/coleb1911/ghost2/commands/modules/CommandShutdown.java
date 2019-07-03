package com.github.coleb1911.ghost2.commands.modules;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.meta.Command;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import discord4j.core.object.util.PermissionSet;

public class CommandShutdown extends Command {
    public CommandShutdown() {
        super("shutdown", CommandType.OPERATOR);
    }

    @Override
    public void invoke(CommandContext ctx) {
        ctx.reply("Bye!");
        Ghost2Application.exit(0);
    }

    @Override
    public PermissionSet getRequiredPermissions() {
        return PermissionSet.none();
    }
}
