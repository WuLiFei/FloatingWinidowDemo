package com.example.mario.floatingwindowtest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import static com.example.mario.floatingwindowtest.AudioState.STATE_PLAYING;

/**
 * Created by MarioStudio on 2016/5/24.
 */

public class MainActivity extends Activity implements View.OnClickListener, MusicService.OnStateChangeListener {

    private ServiceConnection serviceConnection;
    private MusicService musicService;
    private Intent serviceIntent;

    private ImageButton btnPre, btnPla, btnNex;
    private boolean isShowing = false;
    private Button winSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //判断SDK版本是否大于等于19，大于就让他显示，小于就要隐藏，不然低版本会多出来一个
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus();
            //还有设置View的高度，因为每个型号的手机状态栏高度都不相同
        }

        initAllViews();

        bindService();
        startService(serviceIntent);
    }

    @TargetApi(19)
    private void setTranslucentStatus() {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;    // 设置Activity高亮显示
        window.setAttributes(params);
    }

    private void initAllViews() {
        btnPre = (ImageButton) findViewById(R.id.main_previous);
        btnPla = (ImageButton) findViewById(R.id.main_play);
        btnNex = (ImageButton) findViewById(R.id.main_next);
        winSwitch = (Button) findViewById(R.id.main_switch);

        btnPre.setOnClickListener(this);
        btnPla.setOnClickListener(this);
        btnNex.setOnClickListener(this);
        winSwitch.setOnClickListener(this);
    }

    private void bindService() {
        serviceIntent = new Intent(MainActivity.this, MusicService.class);
        if(serviceConnection == null) {
            serviceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    musicService = ((MusicService.WindowBinder)service).getService();
                    musicService.setOnStateChangeListener(MainActivity.this);
                    switchShowState();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if(null != serviceConnection) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        unbindService();
        super.onPause();
    }

    @Override
    protected void onResume() {
        bindService();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        bindService();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        unbindService();
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_play:
                if(musicService.getAudioState() == STATE_PLAYING) {
                    musicService.pause();
                } else {
                    musicService.start();
                }
                break;
            case R.id.main_previous:
                musicService.previous();
                break;
            case R.id.main_next:
                musicService.next();
                break;
            case R.id.main_switch:
                switchShowState();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStateChanged(AudioState audioState) {
        switch (audioState) {
            case STATE_PLAYING:
                btnPla.setImageResource(R.mipmap.landscape_player_btn_pause_normal);
                break;
            default:
                btnPla.setImageResource(R.mipmap.landscape_player_btn_play_press);
                break;
        }
    }

    private void switchShowState() {
        if(isShowing) {
            musicService.dismiss();
            winSwitch.setText("悬浮窗（关闭）");
            isShowing = false;
        } else {
            musicService.show();
            winSwitch.setText("悬浮窗（打开）");
            isShowing = true;
        }
    }
}
