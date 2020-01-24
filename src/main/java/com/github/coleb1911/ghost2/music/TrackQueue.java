package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class TrackQueue extends AudioEventAdapter {
    private static final int MAX_SIZE = 50;

    private final AudioPlayer player;
    private final Deque<AudioTrack> queue;

    TrackQueue(AudioPlayer player) {
        this.player = player;
        this.queue = new ConcurrentLinkedDeque<>();
        player.addListener(this);
    }

    Mono<TrackAddResult> add(AudioTrack track) {
        return Mono.fromCallable(() -> {
            if (queue.size() < MAX_SIZE) {
                if (player.startTrack(track, true)) {
                    return TrackAddResult.PLAYING;
                } else if (queue.offerLast(track)) {
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
                if (s.equals(TrackAddResult.FULL)) return TrackAddResult.MQ_QUEUED_SOME;
                if (s.equals(TrackAddResult.FAILED)) return s;
            }
            return TrackAddResult.MQ_QUEUED_ALL;
        }).onErrorResume(TrackAddResult::failedWithReason);
    }

    Mono<Boolean> shuffle() {
        return Mono.fromCallable(() -> {
            if (queue.isEmpty()) return false;

            List<AudioTrack> tracks = new LinkedList<>(queue);
            Collections.shuffle(tracks);
            queue.clear();
            return queue.addAll(tracks);
        }).onErrorReturn(false);
    }

    Mono<Boolean> remove(int index) {
        return Mono.fromCallable(() -> {
            Optional<AudioTrack> trackAtIndex = queue.stream().skip(index).findFirst();
            if (trackAtIndex.isEmpty()) return false;
            return queue.remove(trackAtIndex.get());
        }).onErrorReturn(false);
    }

    Mono<Boolean> next() {
        return Mono.just(player.startTrack(queue.pollFirst(), false));
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