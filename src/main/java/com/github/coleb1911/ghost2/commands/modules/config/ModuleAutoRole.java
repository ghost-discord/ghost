package com.github.coleb1911.ghost2.commands.modules.config;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;

public final class ModuleAutoRole extends Module {
    @Autowired GuildMetaRepository guildRepo;

    @ReflectiveAccess
    public ModuleAutoRole() {
        super(new ModuleInfo.Builder(ModuleAutoRole.class)
                .withName("autorole")
                .withDescription("Configure automatic role assignment for this guild")
                .withUserPermissions(PermissionSet.of(Permission.MANAGE_ROLES)));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        // Check for arguments
        if (ctx.getArgs().isEmpty() || ctx.getMessage().mentionsEveryone()) {
            ctx.reply("Please specify a role, `enable`, `disable`, or `requireconfirmation [true/false]`.");
            return;
        }

        GuildMeta meta = guildRepo.findById(ctx.getGuild().getId().asLong()).orElseThrow();

        String arg0 = ctx.getArgs().get(0);
        if ("enable".equals(arg0)) {
            if (meta.getAutoRoleId() == null) {
                ctx.reply("Please set a role for autorole before enabling it.");
                return;
            }
            if (meta.getAutoRoleEnabled()) {
                ctx.reply("Autorole is already enabled.");
                return;
            }
            meta.setAutoRoleEnabled(true);
            ctx.reply("Autorole enabled.");
        } else if ("disable".equals(arg0)) {
            if (!meta.getAutoRoleEnabled()) {
                ctx.reply("Autorole is already disabled.");
                return;
            }
            meta.setAutoRoleEnabled(false);
            ctx.reply("Autorole disabled.");
        } else if ("requireconfirmation".equals(arg0)) {
            if (ctx.getArgs().size() < 2) {
                ctx.reply("Please specify \'true\' or \'false\' to enable or disable autorole confirmation.");
                return;
            }

            String arg1 = ctx.getArgs().get(1);
            if ("true".equals(arg1)) {
                if (meta.getAutoRoleConfirmationEnabled()) {
                    ctx.reply("Autorole confirmation is already enabled.");
                    return;
                }
                meta.setAutoRoleConfirmationEnabled(true);
                ctx.reply("Autorole confirmation enabled.");
            } else if ("false".equals(arg1)) {
                if (!meta.getAutoRoleConfirmationEnabled()) {
                    ctx.reply("Autorole confirmation is already disabled.");
                    return;
                }
                meta.setAutoRoleConfirmationEnabled(false);
                ctx.reply("Autorole confirmation disabled.");
            }
        } else if (!ctx.getRoleMentions().isEmpty()) {
            Role role = ctx.getRoleMentions().get(0);
            Role highest = ctx.getSelf().getHighestRole().block();
            if (null == highest || role.getRawPosition() > highest.getRawPosition()) {
                ctx.reply("That role is higher than my highest role. I can't use it.");
                return;
            }
            meta.setAutoRoleId(role.getId().asLong());
            ctx.reply("Autorole set to " + role.getMention() + ".");
        }

        guildRepo.save(meta);
    }
}
