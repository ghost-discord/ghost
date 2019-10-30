package com.github.coleb1911.ghost2.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.github.coleb1911.ghost2.commands.meta.Module.REACT_OK;
import static com.github.coleb1911.ghost2.commands.meta.Module.REACT_WARNING;

public final class MusicUtils {
    private static final String REPLY_BUSY = "I'm already playing music somewhere else.";
    private static final String REPLY_NO_PERMISSIONS = "I don't have permission to use your current voice channel.";
    private static final String REPLY_NO_CHANNEL = "You are not in a voice channel.";

    // Non-instantiable
    private MusicUtils() {
    }

    /**
     * Gets a {@linkplain MusicService} by a user's demand. Performs preliminary
     * permission checks, replies dynamically, and gives reaction-based feedback.
     * <br/>
     * Will not create a new MusicService if one is already active for the guild.
     *
     * @return A {@linkplain Mono} that will emit the resultant MusicService. Nullable.
     */
    public static Mono<MusicService> fetchMusicService(CommandContext ctx) {
        return MusicUtils.getVoiceChannel(ctx.getInvoker())
                // Reply and complete if no channel found
                .switchIfEmpty(Mono.fromRunnable(() -> ctx.reply(REPLY_NO_CHANNEL)))
                // Reply and complete if permissions are insufficient
                .filter(channel -> MusicUtils.checkVoicePermissions(ctx.getInvoker(), channel))
                .switchIfEmpty(Mono.fromRunnable(() -> ctx.reply(REPLY_NO_PERMISSIONS)))
                // Return music service
                .flatMap(channel -> MusicServiceManager.fetch(ctx.getGuild().getId(), channel.getId()))
                // Reaction feedback for normal completion
                .doOnSuccess(ignore -> ctx.getMessage().addReaction(REACT_OK).subscribe())
                // Reaction feedback for unexpected error
                .doOnError(ignore -> ctx.getMessage().addReaction(REACT_WARNING).subscribe());
    }

    /**
     * Gets a {@linkplain Member}'s current {@linkplain VoiceChannel voice channel}.
     *
     * @param member The Member
     * @return The VoiceChannel, or {@link Mono#empty()} if null
     */
    private static Mono<VoiceChannel> getVoiceChannel(Member member) {
        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .filter(Objects::nonNull);
    }

    /**
     * Checks if the given {@linkplain Member} has both {@link Permission#CONNECT}
     * and {@link Permission#SPEAK} in the given {@linkplain VoiceChannel}.
     */
    @SuppressWarnings("ConstantConditions")
    private static boolean checkVoicePermissions(Member member, VoiceChannel channel) {
        return channel.getEffectivePermissions(member.getId())
                .map(permissions -> permissions.contains(Permission.SPEAK) &&
                        permissions.contains(Permission.CONNECT))
                .filter(Objects::nonNull)
                .defaultIfEmpty(false)
                .block();
    }
}