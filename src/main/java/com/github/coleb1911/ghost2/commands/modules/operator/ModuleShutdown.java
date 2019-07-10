package com.github.coleb1911.ghost2.commands.modules.operator;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;

public class ModuleShutdown extends Module {
    public ModuleShutdown() {
        super(new ModuleInfo.Builder(ModuleShutdown.class)
                .withName("shutdown")
                .withDescription("Shut down the bot"));
    }

    @Override
    public void invoke(CommandContext ctx) {
        ctx.reply("Bye!");
        Ghost2Application.getApplicationInstance().exit(0);
    }
}
