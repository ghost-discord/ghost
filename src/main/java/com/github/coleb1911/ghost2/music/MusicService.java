package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

public final class MusicService {
    private final AudioPlayer player;
    private final VoiceSession session;
    private final TrackQueue queue;

    MusicService(AudioPlayer player, VoiceSession session) {
        this.player = player;
        this.session = session;
        this.queue = new TrackQueue(player);
    }

    public Mono<TrackAddResult> loadTrack(String source) {
        return MusicServiceManager.loadFrom(source)
                .filter(Predicate.not(List::isEmpty))
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
        session.leave().subscribe();
        player.destroy();
        queue.destroy();
    }

    boolean shouldCleanup() {
        return (player.getPlayingTrack() == null || player.isPaused());
    }
}