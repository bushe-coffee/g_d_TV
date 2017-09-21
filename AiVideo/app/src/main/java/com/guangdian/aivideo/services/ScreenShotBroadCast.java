package com.guangdian.aivideo.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


import com.guangdian.aivideo.utils.YiPlusUtilities;


public class ScreenShotBroadCast extends BroadcastReceiver {

    private static final String SCREEN_CAP_ACTION = "com.gw.cbn.screencap";
    public static final String START_SERVICE = "com.yiplus.service";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (!YiPlusUtilities.isStringNullOrEmpty(action) && action.equals(SCREEN_CAP_ACTION)) {
            Uri uri = Uri.parse("yiplus://analysis.service");
            Intent intent1 = new Intent();
            intent1.setData(uri);
            intent1.putExtra("StartScreenCap", true);
            context.startService(intent1);
        }

    }
}