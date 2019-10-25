package com.github.coleb1911.ghost2.music;

import com.github.coleb1911.ghost2.References;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public final class MusicService {
    private final AudioPlayer player;
    private final VoiceSession session;
    private final TrackQueue queue;

    MusicService(AudioPlayer player, Snowflake guildId, VoiceSession session) {
        this.player = player;
        this.session = session;
        this.queue = new TrackQueue(player);

        // Request cleanup when voice channel is empty
        DiscordClient client = References.getClient();
        client.getEventDispatcher().on(VoiceStateUpdateEvent.class)
                .filter(e -> e.getClient().equals(client))
                .map(VoiceStateUpdateEvent::getCurrent)
                .flatMap(VoiceState::getChannel)
                .flatMap(VoiceChannel::getVoiceStates)
                .count()
                .filter(count -> (count < 1))
                .thenReturn(guildId)
                .subscribe(MusicServiceManager::requestCleanup);
    }

    public Mono<TrackAddResult> loadTrack(String source) {
        return MusicServiceManager.loadFrom(source)
                .filter(List::isEmpty)
                .flatMap(tracks -> {
                    if (tracks.size() == 1) return queue.add(tracks.get(0));
                    return queue.addAll(tracks);
                })
                .defaultIfEmpty(TrackAddResult.FAILED);
    }

    public Flux<AudioTrack> streamTracks() {
        return queue.getTracks();
    }

    void destroy() {
        session.leave();
        player.destroy();
        queue.destroy();
    }

    boolean shouldCleanup() {
        return (player.getPlayingTrack() == null || player.isPaused());
    }
}