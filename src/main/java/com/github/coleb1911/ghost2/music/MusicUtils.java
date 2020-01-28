package com.github.coleb1911.ghost2.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Permission;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.Objects;

public final class MusicUtils {
    private static final String REPLY_BUSY = "I'm already playing music somewhere else.";
    private static final String REPLY_NO_PERMISSIONS = "I don't have permission to use your current voice channel.";
    private static final String REPLY_NO_CHANNEL = "You are not in a voice channel.";

    // Non-instantiable
    private MusicUtils() {
    }

    /**
     * Gets a {@linkplain MusicService} by a user's demand. Performs preliminary
     * permission checks, and responds dynamically according to status.
     * <br/>
     * Side-effects: this method will create a music service if one does not already exist
     * for the given guild.
     *
     * @return A {@linkplain Mono} that will emit the resultant MusicService. May be empty.
     */
    public static Mono<MusicService> fetchMusicService(CommandContext ctx) {
        return MusicUtils.getVoiceChannel(ctx.getInvoker())
                // Reply and complete if no channel found
                .switchIfEmpty(Mono.fromRunnable(() -> ctx.replyBlocking(REPLY_NO_CHANNEL)))
                // Reply and complete if permissions are insufficient
                .filterWhen(channel -> MusicUtils.checkVoicePermissions(ctx.getSelf(), channel)
                        .doOnNext(canJoin -> {
                            if (!canJoin) ctx.replyBlocking(REPLY_NO_PERMISSIONS);
                        }))
                // Return music service
                .flatMap(channel -> MusicServiceManager.fetch(ctx.getGuild().getId(), channel.getId()));
    }

    /**
     * Gets a {@linkplain Member}'s current {@linkplain VoiceChannel voice channel}.
     *
     * @param member The Member
     * @return The VoiceChannel, or {@link Mono#empty()} if null
     */
    @NonNull
    private static Mono<VoiceChannel> getVoiceChannel(@NonNull Member member) {
        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .filter(Objects::nonNull);
    }

    /**
     * Checks if the given {@linkplain Member} has both {@link Permission#CONNECT}
     * and {@link Permission#SPEAK} in the given {@linkplain VoiceChannel}.
     */
    @NonNull
    private static Mono<Boolean> checkVoicePermissions(@NonNull Member member, @NonNull VoiceChannel channel) {
        return Mono.just(channel)
                .flatMap(c -> c.getEffectivePermissions(member.getId()))
                .map(ps -> ps.contains(Permission.SPEAK) && ps.contains(Permission.CONNECT))
                .defaultIfEmpty(false);
    }
}