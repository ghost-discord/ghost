package com.github.coleb1911.ghost2.music;

public enum TrackAddResult {
    // 0 = playing
    PLAYING(0),
    // 1 = single track
    SQ_QUEUED(1),
    // 2 = multiple tracks
    MQ_QUEUED_ALL(2),
    MQ_QUEUED_SOME(2),
    // 3 = failure
    FULL(3),
    FAILED(3);

    private final int status;

    TrackAddResult(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}