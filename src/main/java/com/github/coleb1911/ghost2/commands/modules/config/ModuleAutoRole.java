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
    private static final String REPLY_INVALID_ARGS = "Please specify a role, `enable`, `disable`, or `requireconfirmation [true/false]`.";
    private static final String REPLY_INVALID_CONFIRM_SETTING = "Please specify \'true\' or \'false\' to enable or disable autorole confirmation.";

    private static final String ARG_ENABLE = "enable";
    private static final String ARG_DISABLE = "disable";
    private static final String ARG_SET_CONFIRM_ENABLED = "requireconfirmation";

    private static final String SETTING_TRUE = "true";
    private static final String SETTING_FALSE = "false";

    @Autowired GuildMetaRepository guildRepo;

    @ReflectiveAccess
    public ModuleAutoRole() {
        super(new ModuleInfo.Builder(ModuleAutoRole.class)
                .withName("autorole")
                .withDescription("Configure automatic role assignment for this guild")
                .withUserPermissions(PermissionSet.of(Permission.MANAGE_ROLES)));
    }

    /**
     * Enables role confirmation with {@code g!confirm}.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private static void enableConfirmation(final CommandContext ctx, final GuildMeta meta) {
        if (meta.getAutoRoleConfirmationEnabled()) {
            ctx.reply("Autorole confirmation is already enabled.");
            return;
        }
        meta.setAutoRoleConfirmationEnabled(true);
        ctx.reply("Autorole confirmation enabled.");
    }

    /**
     * Disables role confirmation with {@code g!confirm}.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private static void disableConfirmation(final CommandContext ctx, final GuildMeta meta) {
        if (!meta.getAutoRoleConfirmationEnabled()) {
            ctx.reply("Autorole confirmation is already disabled.");
            return;
        }
        meta.setAutoRoleConfirmationEnabled(false);
        ctx.reply("Autorole confirmation disabled.");
    }

    /**
     * Set the role for autorole.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private static void setRole(final CommandContext ctx, final GuildMeta meta) {
        Role role = ctx.getRoleMentions().get(0);
        Role highest = ctx.getSelf().getHighestRole().block();

        if (highest == null || role.getRawPosition() > highest.getRawPosition()) {
            ctx.reply("That role is higher than my highest role. I can't use it.");
            return;
        }

        meta.setAutoRoleId(role.getId().asLong());
        ctx.reply("Autorole set to " + role.getMention() + ".");
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for arguments
        if (ctx.getArgs().isEmpty() || ctx.getMessage().mentionsEveryone()) {
            ctx.reply(REPLY_INVALID_ARGS);
            return;
        }

        GuildMeta meta = guildRepo.findById(ctx.getGuild().getId().asLong()).orElseThrow();

        switch (ctx.getArgs().get(0)) {
            case ARG_ENABLE:
                enableAutoRole(ctx, meta);
                break;
            case ARG_DISABLE:
                disableAutoRole(ctx, meta);
                break;
            case ARG_SET_CONFIRM_ENABLED:
                if (ctx.getArgs().size() < 2) {
                    ctx.reply(REPLY_INVALID_CONFIRM_SETTING);
                    return;
                }
                switch (ctx.getArgs().get(1)) {
                    case SETTING_TRUE:
                        enableConfirmation(ctx, meta);
                        break;
                    case SETTING_FALSE:
                        disableConfirmation(ctx, meta);
                        break;
                    default:
                        ctx.reply(REPLY_INVALID_CONFIRM_SETTING);
                        break;
                }
                break;
            default:
                if (!ctx.getRoleMentions().isEmpty()) {
                    setRole(ctx, meta);
                }
        }

        guildRepo.save(meta);
    }

    /**
     * Enables autorole entirely.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private void enableAutoRole(final CommandContext ctx, final GuildMeta meta) {
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
    }

    /**
     * Disables autorole entirely.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private void disableAutoRole(final CommandContext ctx, final GuildMeta meta) {
        if (!meta.getAutoRoleEnabled()) {
            ctx.reply("Autorole is already disabled.");
            return;
        }
        meta.setAutoRoleEnabled(false);
        ctx.reply("Autorole disabled.");
    }
}