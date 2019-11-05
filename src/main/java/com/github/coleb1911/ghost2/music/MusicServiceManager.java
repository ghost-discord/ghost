package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.util.Snowflake;
import org.pmw.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class MusicServiceManager {
    private static final AudioPlayerManager PLAYER_MANAGER;
    private static final ScheduledExecutorService CLEANUP_SCHEDULER;
    private static final Map<Snowflake, MusicService> SERVICES;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);

        CLEANUP_SCHEDULER = new ScheduledThreadPoolExecutor(2);
        CLEANUP_SCHEDULER.scheduleAtFixedRate(MusicServiceManager::cleanupAll, 5L, 5L, TimeUnit.MINUTES);

        SERVICES = new ConcurrentHashMap<>();
        Logger.info("MusicServiceManager initialized.");
    }

    // Disable instantiation
    private MusicServiceManager() {
    }

    public static Mono<MusicService> fetch(final Snowflake guildId, final Snowflake channelId) {
        return Mono.fromCallable(() -> {
            // Return active service if it already exists
            MusicService service;
            if ((service = SERVICES.get(guildId)) != null) return service;

            // Create player, voice session, and audio provider
            AudioPlayer player = PLAYER_MANAGER.createPlayer();
            VoiceSession session = new VoiceSession();
            session.join(channelId, new SimpleAudioProvider(player)).subscribe();

            // Create and return service
            service = new MusicService(player, session);
            SERVICES.put(guildId, service);
            Logger.info("Created new MusicService for guild " + guildId.asString());
            return service;
        });
    }

    /**
     * Check if there is an active {@link MusicService} for the given guild.
     */
    public static boolean serviceExists(final Snowflake guildId) {
        return SERVICES.containsKey(guildId);
    }

    /**
     * Perform full shutdown of the MusicServiceManager.
     */
    public static void shutdown() {
        Flux.fromIterable(SERVICES.keySet())
                .flatMap(MusicServiceManager::forceCleanup)
                .timeout(Duration.ofMinutes(1L))
                .subscribe();
        PLAYER_MANAGER.shutdown();
        CLEANUP_SCHEDULER.shutdown();
    }

    /**
     * Performs MusicService cleanup for a guild. Unconditional.
     *
     * @param guildId Guild to run cleanup on
     */
    public static Mono<Void> forceCleanup(final Snowflake guildId) {
        return Mono.just(guildId)
                .filter(SERVICES::containsKey)
                .map(SERVICES::remove)
                .doOnNext(service -> {
                    service.destroy();
                    Logger.info("Cleaned up MusicService for guild " + guildId.asString());
                })
                .then();
    }

    /**
     * Request to load a track (or tracks) from a LavaPlayer-supported source.
     *
     * @param source Audio track source
     * @return List of audio track(s) acquired from the source
     * @see <a href="https://github.com/sedmelluq/lavaplayer#supported-formats">LavaPlayer formats</a>
     */
    static Mono<List<AudioTrack>> loadFrom(final String source) {
        return Mono.fromCallable(() -> {
            final AtomicReference<List<AudioTrack>> loadResult = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);

            PLAYER_MANAGER.loadItem(source, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    loadResult.set(List.of(track));
                    latch.countDown();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    loadResult.set(List.copyOf(playlist.getTracks()));
                    latch.countDown();
                }

                @Override
                public void noMatches() {
                    Logger.error("Failed to find valid audio track for source " + source);
                    loadResult.set(List.of());
                    latch.countDown();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    Logger.error(e, "Encountered an exception when loading track from source " + source);
                    loadResult.set(List.of());
                    latch.countDown();
                }
            });

            latch.await();
            return loadResult.get();
        }).onErrorReturn(List.of());
    }

    /**
     * Runs cleanup on all {@linkplain MusicService}s currently contained in the
     * cache. Only cleans up MusicServices that should be cleaned up, as dictated by
     * {@link MusicService#shouldCleanup()}.
     */
    private static void cleanupAll() {
        AtomicInteger count = new AtomicInteger(0);
        Flux.fromIterable(SERVICES.keySet())
                .filter(id -> SERVICES.get(id).shouldCleanup())
                .doOnNext(ignore -> count.getAndIncrement())
                .flatMap(MusicServiceManager::forceCleanup)
                .timeout(Duration.ofMinutes(1L))
                .doOnComplete(() -> Logger.info("Cleanup ran. " + count.get() + " services destroyed."))
                .subscribe();
    }
}