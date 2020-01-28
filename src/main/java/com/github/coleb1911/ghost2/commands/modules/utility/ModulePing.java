package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;

public final class ModulePing extends Module {
    @ReflectiveAccess
    public ModulePing() {
        super(new ModuleInfo.Builder(ModulePing.class)
                .withName("ping")
                .withDescription("Check for bot responsiveness"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        ctx.replyBlocking("Pong!");
    }
}