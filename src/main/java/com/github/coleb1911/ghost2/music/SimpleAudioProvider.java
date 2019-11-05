package com.github.coleb1911.ghost2.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;

import java.nio.ByteBuffer;

public final class SimpleAudioProvider extends AudioProvider {
    private final MutableAudioFrame frame = new MutableAudioFrame();
    private final AudioPlayer player;

    SimpleAudioProvider(AudioPlayer player) {
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        this.player = player;

        frame.setBuffer(getBuffer());
    }

    @Override
    public boolean provide() {
        boolean provided = player.provide(frame);
        if (provided)
            getBuffer().flip();
        return provided;
    }
}