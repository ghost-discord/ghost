package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MusicService {
    private final ScheduledExecutorService cleanupScheduler;
    private final Snowflake guildId;
    private final AudioPlayer player;
    private final VoiceSession session;
    private final TrackQueue queue;

    MusicService(Snowflake guildId, AudioPlayer player, VoiceSession session) {
        cleanupScheduler = new ScheduledThreadPoolExecutor(1);
        cleanupScheduler.scheduleAtFixedRate(this::attemptCleanup, 5L, 5L, TimeUnit.MINUTES);

        this.guildId = guildId;
        this.player = player;
        this.session = session;
        this.queue = new TrackQueue(player);
    }

    public Mono<TrackAddResult> loadTrack(String source) {
        return MusicServiceManager.loadFrom(source)
                .flatMap(tracks -> {
                    if (tracks.size() == 0) return TrackAddResult.failedWithReason("Track invalid or not found.");
                    if (tracks.size() == 1) return queue.add(tracks.get(0));
                    return queue.addAll(tracks);
                });
    }

    public Mono<Boolean> next() {
        return queue.next();
    }

    public Mono<Boolean> remove(int index) {
        return queue.remove(index);
    }

    public Flux<AudioTrack> streamTracks() {
        return queue.getTracks();
    }

    public Mono<Boolean> shuffle() {
        return queue.shuffle();
    }

    void destroy() {
        session.leave().subscribe();
        player.destroy();
        queue.destroy();
        cleanupScheduler.shutdown();
    }

    void attemptCleanup() {
        Mono.just(this)
                .filter(MusicService::shouldCleanup)
                .thenReturn(guildId)
                .flatMap(MusicServiceManager::forceCleanup)
                .subscribe();
    }

    boolean shouldCleanup() {
        return ((player.getPlayingTrack() == null && queue.isEmpty()) || player.isPaused());
    }
}