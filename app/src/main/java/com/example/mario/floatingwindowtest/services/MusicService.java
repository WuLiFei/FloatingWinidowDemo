package com.example.mario.floatingwindowtest.services;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mario.floatingwindowtest.FloatWindow;
import com.example.mario.floatingwindowtest.utils.ProgressTextUtils;
import com.example.mario.floatingwindowtest.R;

import java.io.IOException;

import static com.example.mario.floatingwindowtest.services.AudioState.STATE_PAUSE;
import static com.example.mario.floatingwindowtest.services.AudioState.STATE_PLAYING;
import static com.example.mario.floatingwindowtest.services.AudioState.STATE_PREPARE;
import static com.example.mario.floatingwindowtest.services.AudioState.STATE_READY;
import static com.example.mario.floatingwindowtest.services.AudioState.STATE_STOP;

/**
 * Created by MarioStudio on 2016/5/23.
 */

public class MusicService extends Service implements PlayerInterface, View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener {

    private int position;
    private boolean first = true;
    private boolean flag, userTouch;
    private MediaPlayer mediaPlayer;

    private static final int WHAT_REFRESH = 0x268;

    private SeekBar progress;
    private AudioState touchState;
    private FloatWindow floatWindow;
    private View menuView, floatView;
    private ImageView imgController, imgCD;
    private ImageButton btnPre, btnPla, btnNex;
    private AudioState audioState = AudioState.STATE_STOP;
    private TextView textDisplay, textPosition, textDuration;

    private String res_names[] = null;
    private String dis_names[] = null;

    private AnimatorState animatorState = AnimatorState.STATE_END;
    private OnStateChangeListener onStateChangeListener;

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initFloatWindow();
        dis_names = this.getResources().getStringArray(R.array.dis_names);
        res_names = this.getResources().getStringArray(R.array.res_names);
        position = 0;
        audioStart();
    }

    private void initFloatWindow() {
        floatView = LayoutInflater.from(this).inflate(R.layout.layout_float, null);
        menuView = LayoutInflater.from(this).inflate(R.layout.layout_menu, null);

        btnPre = (ImageButton) menuView.findViewById(R.id.player_previous);
        btnPla = (ImageButton) menuView.findViewById(R.id.player_play);
        btnNex = (ImageButton) menuView.findViewById(R.id.player_next);
        textDuration = (TextView) menuView.findViewById(R.id.player_duration);
        textPosition = (TextView) menuView.findViewById(R.id.player_progress);
        textDisplay = (TextView) menuView.findViewById(R.id.player_displayname);
        progress = (SeekBar) menuView.findViewById(R.id.player_seek);
        progress.setOnSeekBarChangeListener(this);
        btnPre.setOnClickListener(this);
        btnPla.setOnClickListener(this);
        btnNex.setOnClickListener(this);

        imgCD = (ImageView) floatView.findViewById(R.id.mini_cd);
        imgController = (ImageView) floatView.findViewById(R.id.mini_handle);

        floatWindow = new FloatWindow(this);
        floatWindow.setFloatView(floatView);
        floatWindow.setPlayerView(menuView);
    }

    /**
     * 打开悬浮窗
     * */
    public void show() {
        if(null != floatWindow) {
            floatWindow.show();
        }
    }

    /**
     * 关闭悬浮窗
     * */
    public void dismiss() {
        if(null != floatWindow) {
            floatWindow.dismiss();
        }
    }

    @Override
    public void start() {
        if(audioState == STATE_PAUSE || audioState == STATE_READY) {
            startAnimator().start();
        } else {
            audioStart();
        }
    }

    @Override
    public void pause() {
        pauseAnimator().start();
    }

    @Override
    public void previous() {
        if(animatorState == AnimatorState.STATE_END && audioState != STATE_PREPARE) {
            imgCD.getDrawable().setLevel(0);
            // 判断如果当前处于播放状态的话，给一个停止动画，否则直接进行播放动画
            if(audioState == STATE_PLAYING) {
                stopAnimator(-1).start();
            } else {
                audioPrepare();
            }
        }
    }

    @Override
    public void next() {
        if(animatorState == AnimatorState.STATE_END && audioState != STATE_PREPARE) {
            imgCD.getDrawable().setLevel(0);
            // 判断如果当前处于播放状态的话，给一个停止动画，否则直接进行播放动画
            if(audioState == STATE_PLAYING) {
                stopAnimator(1).start();
            } else {
                audioNext();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if(animatorState == AnimatorState.STATE_END) {
            switch (view.getId()) {
                case R.id.player_previous:
                    previous();
                    break;
                case R.id.player_play:
                    if(audioState == STATE_PLAYING) {
                        pause();
                    } else {
                        start();
                    }
                    break;
                case R.id.player_next:
                    next();
                    break;
                default:
                    break;
            }
        }
    }

    private void audioPrepare() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setDataSource(res_names[position]);
            textDisplay.setText(dis_names[position]);  //  设置显示
            setAudioState(STATE_PREPARE);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void audioNext() {
        position ++;
        if(position >= res_names.length) {
            position = 0;
        }
        audioPrepare();
    }

    private void audioPrevious() {
        position --;
        if(position < 0) {
            position = res_names.length - 1;
        }
        audioPrepare();
    }

    private void audioStop() {
        flag = false;
        handler.removeMessages(WHAT_REFRESH);
        if(audioState == AudioState.STATE_STOP) {
            return;
        }
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            setAudioState(AudioState.STATE_STOP);
        }
    }

    private void audioPause() {
        if(audioState == STATE_PLAYING) {
            mediaPlayer.pause();
            setAudioState(AudioState.STATE_PAUSE);
        }
    }

    private void audioStart() {
        switch (audioState) {
            case STATE_PLAYING:
                break;
            case STATE_PREPARE:
                break;
            case STATE_STOP:
                audioPrepare();
                break;
            case STATE_READY:
                mediaPlayer.start();
                setAudioState(STATE_PLAYING);
                break;
            case STATE_PAUSE:
                mediaPlayer.start();
                setAudioState(STATE_PLAYING);
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopAnimator(1).start();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        setAudioState(AudioState.STATE_READY);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        progress.setSecondaryProgress(progress.getMax() * percent / 100);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser && mediaPlayer!=null && touchState != STATE_STOP) {
            mediaPlayer.seekTo(progress);
            textPosition.setText(ProgressTextUtils.getProgressText(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userTouch = true;
        touchState = audioState;
        pauseAnimator().start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        userTouch = false;
        switch (touchState) {
            case STATE_PLAYING:
                startAnimator().start();
                break;
        }
    }

    @Override
    public AudioState getAudioState() {
        return audioState;
    }

    @Override
    public void setAudioState(AudioState audioState) {
        if(this.audioState == audioState) {
            return;
        }
        this.audioState = audioState;
        if(onStateChangeListener != null) {
            onStateChangeListener.onStateChanged(audioState);
        }
        switch (audioState) {
            case STATE_PLAYING:
                isPlaying();
                break;
            case STATE_PREPARE:
                isPrepare();
                break;
            case STATE_READY:
                isReady();
                break;
            case STATE_PAUSE:
                isPause();
                break;
            case STATE_STOP:
                isStop();
                break;
        }
    }

    /**
     * 设置准备开始状态相关信息
     * */
    private void isPrepare() {

    }

    /**
     * 设置播放状态相关信息
     * */
    private void isPlaying() {
        flag = true;
        handler.sendEmptyMessage(WHAT_REFRESH);
        btnPla.setImageResource(R.mipmap.landscape_player_btn_pause_normal);
    }

    /**
     * 设置准备就绪状态相关信息
     * */
    private void isReady() {
        progress.setMax(mediaPlayer.getDuration());
        textDuration.setText(ProgressTextUtils.getProgressText(mediaPlayer.getDuration()));
        textPosition.setText(ProgressTextUtils.getProgressText(mediaPlayer.getCurrentPosition()));
        if(first) {
            first = false;
            return;
        }
        startAnimator().start();
    }

    /**
     * 设置暂停状态相关信息
     * */
    private void isPause() {
        flag = false;
        handler.removeMessages(WHAT_REFRESH);
        btnPla.setImageResource(R.mipmap.landscape_player_btn_play_press);
    }

    /**
     * 设置停止状态相关信息
     * */
    private void isStop() {
        btnPla.setImageResource(R.mipmap.landscape_player_btn_play_press);
        progress.setSecondaryProgress(0);
        progress.setProgress(0);
        progress.setMax(100);
        textPosition.setText("00:00");
        textDuration.setText("00:00");
    }

    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_REFRESH:
                    if(flag) {
                        handler.sendEmptyMessageDelayed(WHAT_REFRESH, 100);
                    }
                    refreshProgress();
                    break;
            }
        }
    };

    /**
     * 开始动画
     * */
    private ValueAnimator startAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 10000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int level = (int) animation.getAnimatedValue();
                imgController.getDrawable().setLevel(level);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                animatorState = AnimatorState.STATE_START;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animatorState = AnimatorState.STATE_END;
                audioStart();
            }
        });
        animator.setDuration(300);
        return animator;
    }

    /**
     * 暂停动画
     * */
    private ValueAnimator pauseAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(10000, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int level = (int) animation.getAnimatedValue();
                imgController.getDrawable().setLevel(level);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                animatorState = AnimatorState.STATE_START;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animatorState = AnimatorState.STATE_END;
                audioPause();
            }
        });
        animator.setDuration(300);
        return animator;
    }

    /**
     * 停止动画， @param trend
     * 参数trend，之所以叫trend，是因为停止动作完成之后有一个后续动作，是播放上一首（-1），下一首（1），还是不做任何操作
     * */
    private ValueAnimator stopAnimator(final int trend) {
        ValueAnimator animator = ValueAnimator.ofInt(10000, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int level = (int) animation.getAnimatedValue();
                imgController.getDrawable().setLevel(level);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                audioStop();
                animatorState = AnimatorState.STATE_START;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animatorState = AnimatorState.STATE_END;
                switch (trend) {
                    case -1:
                        audioPrevious();
                        break;
                    case 1:
                        audioNext();
                        break;
                    default:
                        break;
                }
            }
        });
        animator.setDuration(150);
        return animator;
    }

    /**
     * 刷新进程
     * */
    private void refreshProgress() {
        int level = imgCD.getDrawable().getLevel();
        level = level + 200;
        if(level > 10000) {
            level = level - 10000;
        }
        imgCD.getDrawable().setLevel(level);
        textPosition.setText(ProgressTextUtils.getProgressText(mediaPlayer.getCurrentPosition()));
        if(!userTouch) {
            progress.setProgress(mediaPlayer.getCurrentPosition());
        }
    }

    private enum AnimatorState {
        STATE_START, STATE_END;
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    public interface OnStateChangeListener {
        public void onStateChanged(AudioState audioState);
    }
}
