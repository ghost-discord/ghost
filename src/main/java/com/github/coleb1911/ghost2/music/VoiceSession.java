package com.github.coleb1911.ghost2.music;

import com.github.coleb1911.ghost2.References;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public final class VoiceSession {
    private final AtomicReference<VoiceConnection> connection = new AtomicReference<>();
    private final AtomicReference<VoiceSession.State> state = new AtomicReference<>();

    Mono<Void> join(final Snowflake channelId, final AudioProvider provider) {
        return References.getClient().getChannelById(channelId)
                .cast(VoiceChannel.class)
                .flatMap(ch -> ch.join(spec -> {
                    spec.setSelfDeaf(true);
                    spec.setProvider(provider);
                    state.set(State.CONNECTING);
                }))
                .timeout(Duration.ofSeconds(10L))
                .doOnNext(connection::set)
                .thenReturn(State.CONNECTED)
                .doOnSuccess(state::set)
                .then();
    }

    Mono<Void> leave() {
        return Mono.fromRunnable(() -> {
            connection.get().disconnect();
            connection.set(null);
        }).doOnSuccess(ignore -> state.set(State.DISCONNECTED)).then();
    }

    State getState() {
        return state.get();
    }

    public enum State {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }
}
