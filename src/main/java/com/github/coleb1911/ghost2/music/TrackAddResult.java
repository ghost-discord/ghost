package com.github.coleb1911.ghost2.music;

public enum TrackAddResult {
    // Single track, immediate play
    PLAYING("Playing track."),
    // Single track
    SQ_QUEUED("Queued track."),
    // Multiple tracks
    MQ_QUEUED_ALL("Queued playlist."),
    MQ_QUEUED_SOME("Queued playlist. Some tracks could not be added to the queue."),
    // Failure
    FULL("The queue is full."),
    FAILED("An error occurred. Please try again.");

    public final String message;

    TrackAddResult(final String message) {
        this.message = message;
    }
}