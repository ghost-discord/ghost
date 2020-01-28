package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;

public final class ModuleUptime extends Module {
    @ReflectiveAccess
    public ModuleUptime() {
        super(new ModuleInfo.Builder(ModuleUptime.class)
                .withName("uptime")
                .withDescription("Returns the bot uptime"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        ctx.replyBlocking(References.uptime());
    }
}
