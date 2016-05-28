package com.example.mario.floatingwindowtest;

/**
 * Created by MarioStudio on 2016/5/25.
 */

public interface PlayerInterface {


    /**
     * 开始播放
     * */
    public void start();

    /**
     * 暂停播放
     * */
    public void pause();

    /**
     * 播 上一首
     * */
    public void previous();

    /**
     * 播 下一首
     * */
    public void next();

    /**
     * 设置播放状态
     * */
    public void setAudioState(AudioState audioState);

    /**
     * 获得播放状态
     * */
    public AudioState getAudioState();

}
