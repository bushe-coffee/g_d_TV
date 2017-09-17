package com.guangdian.dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class OpenActivityBroadCast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("YI plus  onReceive  ");
        if (intent != null) {
            String path = intent.getStringExtra("ImagePath");
            Intent intent1 = new Intent(context, CustomerService.class);
//            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            intent1.putExtra("ImagePath", path);
            System.out.println("YI plus  startActivity  ");
            context.startService(intent1);
        }

        // 终止广播
//        abortBroadcast();
    }


}
