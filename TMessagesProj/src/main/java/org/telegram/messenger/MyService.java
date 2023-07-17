package org.telegram.messenger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;


import androidx.annotation.Nullable;


public class MyService extends Service {

    private BroadcastReceiver vReceiver;
    @Override
    public void onCreate() {
        super.onCreate();
        vReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                FileLog.e("SOME SOME SOME SOME SOME");
            }
        };
        registerReceiver(vReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(vReceiver);
    }
}
