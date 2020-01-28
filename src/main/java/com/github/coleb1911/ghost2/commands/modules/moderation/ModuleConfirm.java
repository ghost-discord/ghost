package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.validation.constraints.NotNull;

@Configurable
public final class ModuleConfirm extends Module {
    @Autowired GuildMetaRepository guildRepo;

    @ReflectiveAccess
    public ModuleConfirm() {
        super(new ModuleInfo.Builder(ModuleConfirm.class)
                .withName("confirm")
                .withDescription("Gives you your default role")
                .withBotPermissions(PermissionSet.of(Permission.MANAGE_ROLES)));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        ctx.getSelf().getBasePermissions().subscribe(permissions -> {
            if (permissions.contains(Permission.MANAGE_MESSAGES))
                ctx.getMessage().delete().subscribe();
        });

        GuildMeta meta = guildRepo.findById(ctx.getGuild().getId().asLong()).orElseThrow();
        if (meta.getAutoRoleConfirmationEnabled()) {
            Role role = ctx.getGuild().getRoleById(Snowflake.of(meta.getAutoRoleId())).block();
            Role highest = ctx.getSelf().getHighestRole().block();
            if (null == role || null == highest || role.getRawPosition() > highest.getRawPosition()) {
                ctx.replyBlocking("Autorole is configured to use an invalid role. Notify an admin.");
                return;
            }

            if (!ctx.getInvoker().getRoleIds().contains(role.getId())) {
                ctx.getInvoker().addRole(role.getId(), "Autorole").subscribe();
                ctx.replyDirectBlocking("You have received your role.");
            } else {
                ctx.replyDirectBlocking("You already have the " + role.getName() + " role.");
            }
        } else {
            ctx.replyDirectBlocking("Autorole confirmation is disabled. Your roles have not changed.");
        }
    }
}
