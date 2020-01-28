package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class TrackQueue extends AudioEventAdapter {
    private static final int MAX_SIZE = 50;

    private final AudioPlayer player;
    private final List<AudioTrack> queue;

    TrackQueue(AudioPlayer player) {
        this.player = player;
        this.queue = Collections.synchronizedList(new LinkedList<>());
        player.addListener(this);
    }

    Mono<TrackAddResult> add(AudioTrack track) {
        return Mono.fromCallable(() -> {
            if (queue.size() < MAX_SIZE) {
                if (player.startTrack(track, true)) {
                    return TrackAddResult.PLAYING;
                } else {
                    queue.add(track);
                    return TrackAddResult.SQ_QUEUED;
                }
            }
            return TrackAddResult.FULL;
        }).onErrorResume(TrackAddResult::failedWithReason);
    }

    Mono<TrackAddResult> addAll(List<AudioTrack> tracks) {
        return Mono.fromCallable(() -> {
            List<TrackAddResult> stats = Flux.fromIterable(tracks)
                    .take(MAX_SIZE)
                    .flatMap(this::add)
                    .collectList()
                    .block();

            if (tracks.size() > MAX_SIZE) return TrackAddResult.MQ_QUEUED_SOME;

            for (TrackAddResult s : stats) {
                if (TrackAddResult.FULL.equals(s)) return TrackAddResult.MQ_QUEUED_SOME;
                if (TrackAddResult.FAILED.equals(s)) return s;
            }
            return TrackAddResult.MQ_QUEUED_ALL;
        }).onErrorResume(TrackAddResult::failedWithReason);
    }

    Mono<Boolean> shuffle() {
        return Mono.fromCallable(() -> {
            Collections.shuffle(queue);
            return true;
        }).onErrorReturn(false);
    }

    Mono<AudioTrack> remove(int index) {
        return Mono.just(queue.remove(index));
    }

    Mono<Boolean> next() {
        final AudioTrack track = queue.isEmpty() ? null : queue.remove(0);
        return Mono.just(player.startTrack(track, false));
    }

    Flux<AudioTrack> getTracks() {
        return Flux.fromIterable(List.copyOf(queue));
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    void destroy() {
        queue.clear();
        player.removeListener(this);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Mono.just(endReason.mayStartNext)
                .filter(Boolean::booleanValue)
                .flatMap(ignore -> next())
                .subscribe();
    }
}