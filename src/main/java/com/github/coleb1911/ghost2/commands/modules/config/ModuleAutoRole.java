package com.github.coleb1911.ghost2.commands.modules.config;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.EventHandler;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.validation.constraints.NotNull;

@Configurable
public final class ModuleAutoRole extends Module {
    private static final String ARG_ENABLE = "enable";
    private static final String ARG_DISABLE = "disable";
    private static final String ARG_SET_CONFIRM_ENABLED = "confirm";

    private static final String REPLY_INVALID_ARGS = String.format("Please specify a role, `%s`, `%s`, or `%s [%s/%s]`.", ARG_ENABLE, ARG_DISABLE, ARG_SET_CONFIRM_ENABLED, ARG_ENABLE, ARG_DISABLE);
    private static final String REPLY_INVALID_CONFIRM_SETTING = String.format("Please specify `%s` or `%s` to enable or disable autorole confirmation.", ARG_ENABLE, ARG_DISABLE);

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
            ctx.replyBlocking("Autorole confirmation is already enabled.");
            return;
        }
        meta.setAutoRoleConfirmationEnabled(true);
        ctx.replyBlocking("Autorole confirmation enabled.");
    }

    /**
     * Disables role confirmation with {@code g!confirm}.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private static void disableConfirmation(final CommandContext ctx, final GuildMeta meta) {
        if (!meta.getAutoRoleConfirmationEnabled()) {
            ctx.replyBlocking("Autorole confirmation is already disabled.");
            return;
        }
        meta.setAutoRoleConfirmationEnabled(false);
        ctx.replyBlocking("Autorole confirmation disabled.");
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
            ctx.replyBlocking("That role is higher than my highest role. I can't use it.");
            return;
        }

        meta.setAutoRoleId(role.getId().asLong());
        ctx.replyBlocking("Autorole set to " + role.getMention() + ".");
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for arguments
        if (ctx.getArgs().isEmpty() || ctx.getMessage().mentionsEveryone()) {
            ctx.replyBlocking(REPLY_INVALID_ARGS);
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
                    ctx.replyBlocking(REPLY_INVALID_CONFIRM_SETTING);
                    return;
                }
                switch (ctx.getArgs().get(1)) {
                    case ARG_ENABLE:
                        enableConfirmation(ctx, meta);
                        break;
                    case ARG_DISABLE:
                        disableConfirmation(ctx, meta);
                        break;
                    default:
                        ctx.replyBlocking(REPLY_INVALID_CONFIRM_SETTING);
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
            ctx.replyBlocking("Please set a role for autorole before enabling it.");
            return;
        }

        if (meta.getAutoRoleEnabled()) {
            ctx.replyBlocking("Autorole is already enabled.");
            return;
        }
        meta.setAutoRoleEnabled(true);
        ctx.replyBlocking("Autorole enabled.");
    }

    /**
     * Disables autorole entirely.
     *
     * @param ctx  Command context from {@code invoke}
     * @param meta {@link GuildMeta} from {@code invoke}
     */
    private void disableAutoRole(final CommandContext ctx, final GuildMeta meta) {
        if (!meta.getAutoRoleEnabled()) {
            ctx.replyBlocking("Autorole is already disabled.");
            return;
        }
        meta.setAutoRoleEnabled(false);
        ctx.replyBlocking("Autorole disabled.");
    }

    @EventHandler(MemberJoinEvent.class)
    @ReflectiveAccess
    public void onEvent(MemberJoinEvent event) {
        GuildMeta meta = guildRepo.findById(event.getGuildId().asLong()).orElse(null);
        if (meta == null) {
            throw new IllegalStateException("Guild {} doesn't exist in database");
        }

        if (meta.getAutoRoleEnabled() && !meta.getAutoRoleConfirmationEnabled()) {
            Snowflake roleId = Snowflake.of(meta.getAutoRoleId());
            Guild guild = event.getGuild().blockOptional().orElseThrow();

            event.getMember().addRole(roleId, "Autorole").subscribe();
            String dm = "Welcome to " + guild.getName() + "! You've received the " + guild.getRoleById(roleId).map(Role::getName).block() + " role.";
            event.getMember().getPrivateChannel().subscribe(c -> c.createMessage(dm).subscribe());
        }
    }
}