package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;

public class ModulePing extends Module {
    public ModulePing() {
        super(new ModuleInfo.Builder(ModulePing.class)
                .withName("ping")
                .withDescription("Check for bot responsiveness"));
    }

    @Override
    public void invoke(CommandContext ctx) {
        ctx.reply("Pong!");
    }
}