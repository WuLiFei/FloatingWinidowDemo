package com.example.mario.floatingwindowtest.services;

/**
 * Created by MarioStudio on 2016/5/25.
 */

public enum AudioState {

    STATE_PLAYING(0x15), STATE_PREPARE(0x25), STATE_STOP(0x35), STATE_PAUSE(0x45), STATE_READY(0x55);

    private int state;

    AudioState(int state) {
        this.state = state;
    }
}
