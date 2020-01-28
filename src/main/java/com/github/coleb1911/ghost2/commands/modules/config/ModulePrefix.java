package com.github.coleb1911.ghost2.commands.modules.config;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.validation.constraints.NotNull;


@Configurable
public final class ModulePrefix extends Module {
    @Autowired private GuildMetaRepository guildRepo;

    @ReflectiveAccess
    public ModulePrefix() {
        super(new ModuleInfo.Builder(ModulePrefix.class)
                .withName("prefix")
                .withDescription("Set ghost2's prefix for this guild"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for at least one argument
        if (ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("You didn't supply a prefix.");
            return;
        }

        // Get prefix and check length
        String prefix = ctx.getArgs().get(0);
        if (prefix.length() > GuildMeta.PREFIX_LENGTH) {
            ctx.replyBlocking("That prefix is too long. The maximum prefix length is " + GuildMeta.PREFIX_LENGTH + ".");
            return;
        }

        // Save prefix
        GuildMeta meta = guildRepo.findById(ctx.getGuild().getId().asLong()).orElseThrow();
        meta.setPrefix(prefix);
        guildRepo.save(meta);
        ctx.replyBlocking("Set prefix to `" + prefix + "`.");
    }
}
