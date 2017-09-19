package com.guangdian.aivideo.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


import com.guangdian.aivideo.utils.YiPlusUtilities;

import java.io.IOException;
import java.io.OutputStream;


public class ScreenShotBroadCast extends BroadcastReceiver {

    private static final String SCREEN_CAP_ACTION = "com.gw.cbn.screencap";
    public static final String START_SERVICE = "com.yiplus.service";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String cache = context.getCacheDir().getAbsolutePath();
        if (!YiPlusUtilities.isStringNullOrEmpty(action) && action.equals(SCREEN_CAP_ACTION))
            try {
                String adbContent = "/system/bin/screencap -p " + cache + "/screenshot.jpg";
                Process sh = Runtime.getRuntime().exec("su", null, null);
                OutputStream os = sh.getOutputStream();
                os.write(adbContent.getBytes("ASCII"));
                os.flush();
                os.close();
                sh.waitFor();
                System.out.println("majie 2222 " + System.currentTimeMillis());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                String path = cache + "/screenshot.jpg";
                Uri uri = Uri.parse("yiplus://analysis.service");
                Intent intent1 = new Intent();
                intent1.setData(uri);
                intent1.putExtra("ImagePath", path);
                context.startService(intent1);
            }
    }
}
