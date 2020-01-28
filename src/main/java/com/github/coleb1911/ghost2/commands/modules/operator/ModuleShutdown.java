package com.github.coleb1911.ghost2.commands.modules.operator;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;

public final class ModuleShutdown extends Module {
    @ReflectiveAccess
    public ModuleShutdown() {
        super(new ModuleInfo.Builder(ModuleShutdown.class)
                .withName("shutdown")
                .withDescription("Shut down the bot"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        ctx.replyBlocking("Bye!");
        References.getApplicationInstance().exit(0);
    }
}
