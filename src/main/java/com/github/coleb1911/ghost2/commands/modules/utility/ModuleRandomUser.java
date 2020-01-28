package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author mpardoen
 */
public final class ModuleRandomUser extends Module {
    private static final Random RNG = new Random();

    @ReflectiveAccess
    public ModuleRandomUser() {
        super(new ModuleInfo.Builder(ModuleRandomUser.class)
                .withName("randomuser")
                .withDescription("Return a random user")
                .withAliases("ruser"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        VoiceState callerState = ctx.getInvoker().getVoiceState().block();
        List<User> users = new ArrayList<>();

        // If the invoker is currently in a voice channel
        if (callerState != null) {
            users = callerState
                    .getChannel()
                    .map(VoiceChannel::getVoiceStates).blockOptional().orElseThrow()
                    .flatMap(VoiceState::getUser)
                    .collectList()
                    .block();
            // If not, return a random user online in the current text channel
        } else {
            List<Member> allMembers = Mono.justOrEmpty(ctx.getGuild().getMembers().collectList().block())
                    .defaultIfEmpty(List.of())
                    .block();
            assert allMembers != null;
            for (Member member : allMembers) {
                if (member == null) continue;
                Status status = member.getPresence().map(Presence::getStatus).blockOptional().orElseThrow();
                PermissionSet perms = ((TextChannel) ctx.getChannel()).getEffectivePermissions(member.getId()).blockOptional().orElseThrow();

                if (!member.isBot() &&
                        !status.equals(Status.OFFLINE) &&
                        perms.contains(Permission.VIEW_CHANNEL)) {
                    users.add(member);
                }
            }
        }

        if (users != null && !users.isEmpty()) {
            User randomPick = users.get(RNG.nextInt(users.size()));
            ctx.replyBlocking(randomPick.getMention());
        } else ctx.replyBlocking("There are no users to pick from.");
    }
}
