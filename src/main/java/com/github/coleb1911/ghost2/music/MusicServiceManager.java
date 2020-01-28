package com.github.coleb1911.ghost2.music;

import com.github.coleb1911.ghost2.References;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.util.Snowflake;
import org.pmw.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicServiceManager {
    private static final AudioPlayerManager PLAYER_MANAGER;
    private static final Map<Snowflake, MusicService> SERVICES;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);

        SERVICES = new ConcurrentHashMap<>();
        Logger.info("MusicServiceManager initialized.");
    }

    // Disable instantiation
    private MusicServiceManager() {
    }

    public static Mono<MusicService> fetch(final Snowflake guildId, final Snowflake channelId) {
        return Mono.fromCallable(() -> {
            // Return active service if it already exists
            if (SERVICES.get(guildId) != null) return SERVICES.get(guildId);

            // Create player, voice session, and audio provider
            AudioPlayer player = PLAYER_MANAGER.createPlayer();
            VoiceSession session = new VoiceSession();
            session.join(channelId, new SimpleAudioProvider(player)).subscribe();

            // Create and return service
            final MusicService service = new MusicService(guildId, player, session);
            SERVICES.put(guildId, service);
            Logger.info("Created new MusicService for guild " + guildId.asString());

            // Destroy service on disconnect
            final DiscordClient self = References.getClient();
            self.getEventDispatcher().on(VoiceStateUpdateEvent.class)
                    .filter(ev -> ev.getClient().equals(self))
                    .filterWhen(ev -> ev.getCurrent().getChannel().map(Objects::isNull).defaultIfEmpty(true))
                    .map(ignore -> service.getGuildId())
                    .subscribe(MusicServiceManager::forceCleanup);

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
        return Mono.create(sink -> PLAYER_MANAGER.loadItem(source, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                sink.success(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                sink.success(List.copyOf(playlist.getTracks()));
            }

            @Override
            public void noMatches() {
                sink.success();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                Logger.error(e, "Encountered an exception when loading track from source " + source);
                sink.error(e);
            }
        }));
    }
}