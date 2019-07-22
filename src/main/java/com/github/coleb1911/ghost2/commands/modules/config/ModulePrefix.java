package com.github.coleb1911.ghost2.commands.modules.config;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;


public final class ModulePrefix extends Module {
    @Autowired private GuildMetaRepository guildRepo;

    @ReflectiveAccess
    public ModulePrefix() {
        super(new ModuleInfo.Builder(ModulePrefix.class)
                .withName("prefix")
                .withDescription("Set ghost2's prefix for this guild"));
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for at least one argument
        if (ctx.getArgs().isEmpty()) {
            ctx.reply("You didn't supply a prefix.");
            return;
        }

        // Get prefix and check length
        String prefix = ctx.getArgs().get(0);
        if (prefix.length() > GuildMeta.PREFIX_LENGTH) {
            ctx.reply("That prefix is too long. The maximum prefix length is " + GuildMeta.PREFIX_LENGTH +".");
            return;
        }

        // Save prefix
        guildRepo.save(new GuildMeta(ctx.getGuild().getId().asLong(), prefix));
        ctx.reply("Set prefix to `"+ prefix + "`.");
    }
}
