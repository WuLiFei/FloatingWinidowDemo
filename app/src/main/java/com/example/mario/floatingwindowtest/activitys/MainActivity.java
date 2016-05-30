package com.example.mario.floatingwindowtest.activitys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.mario.floatingwindowtest.services.MusicService;
import com.example.mario.floatingwindowtest.R;
import com.example.mario.floatingwindowtest.utils.ApplicationUtils;

/**
 * Created by MarioStudio on 2016/5/24.
 */

public class MainActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {

    private ServiceConnection serviceConnection;
    private Intent serviceIntent;
    private TextView versionName;
    private CheckBox checkBox;

    private MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initAllViews();
        bindService();
        startService(serviceIntent);
    }

    private void initAllViews() {
        checkBox = (CheckBox) findViewById(R.id.main_check_box);
        checkBox.setOnCheckedChangeListener(this);

        versionName = (TextView) findViewById(R.id.main_version_name);
        versionName.setText(ApplicationUtils.getAppVersionName(this));
    }

    private void bindService() {
        serviceIntent = new Intent(MainActivity.this, MusicService.class);
        if(serviceConnection == null) {
            serviceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    musicService = ((MusicService.MusicBinder)service).getService();

                    SharedPreferences preferences = getSharedPreferences("FloatMusicPlayer", Context.MODE_PRIVATE);
                    boolean isCheck = preferences.getBoolean("isCheck", false);
                    checkBox.setChecked(isCheck);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            musicService.show();
        } else {
            musicService.dismiss();
        }
        SharedPreferences preferences = getSharedPreferences("FloatMusicPlayer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isCheck", isChecked);
        editor.commit();
    }
}
