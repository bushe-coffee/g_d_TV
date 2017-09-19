package com.guangdian.dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.guangdian.dialog.utils.YiPlusUtilities;


public class BootBroadCast extends BroadcastReceiver {

    public static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
    public static final String AWAKE_SERVER = "com.yiplus.awake_server";

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("majie   BootBroadCast ");
        String action = intent.getAction();
        if (!YiPlusUtilities.isStringNullOrEmpty(action) &&
                (AWAKE_SERVER.equals(intent.getAction()) || BOOT_ACTION.equals(intent.getAction()))) {
            System.out.println("majie   open boot broadcast and start serves ");
            Intent intent1 = new Intent(context, CustomerService.class);
//            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            context.startService(intent1);
        }
    }


}
