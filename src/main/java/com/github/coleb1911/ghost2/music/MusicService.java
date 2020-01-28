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

    public Snowflake getGuildId() {
        return guildId;
    }

    public Mono<Boolean> next() {
        return queue.next();
    }

    public Mono<AudioTrack> getCurrentTrack() {
        return Mono.just(player.getPlayingTrack());
    }

    public Mono<AudioTrack> remove(int index) {
        return queue.remove(index);
    }

    /**
     * Stream the tracks currently in the queue.
     *
     * @return A {@link Flux<AudioTrack>}
     */
    public Flux<AudioTrack> streamTracks() {
        return queue.getTracks();
    }

    /**
     * Shuffle the items currently in the queue.
     *
     * @return Mono.just(true) if successful
     */
    public Mono<Boolean> shuffle() {
        return queue.shuffle();
    }

    void destroy() {
        session.leave().subscribe();
        player.destroy();
        queue.destroy();
        cleanupScheduler.shutdown();
    }

    private void attemptCleanup() {
        Mono.just(this)
                .filter(MusicService::shouldCleanup)
                .map(svc -> svc.guildId)
                .flatMap(MusicServiceManager::forceCleanup)
                .subscribe();
    }

    private boolean shouldCleanup() {
        return ((player.getPlayingTrack() == null && queue.isEmpty()) || player.isPaused());
    }
}