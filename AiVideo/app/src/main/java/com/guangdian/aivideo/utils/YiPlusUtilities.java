package com.guangdian.aivideo.utils;


import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class YiPlusUtilities {


    public static String LIST_URL = "http://47.94.37.237/api/video/all";
    public static String VIDEO_COMMEND_URL = "http://47.94.37.237/api/recommand";
    public static String ANALYSIS_IMAGE_URL = "http://47.94.37.237/api/analysis";
    private static String ACCESS_KEY = "9aPx3h888D0rWcX20HSayvHqxlvxHbjn";
    private static String SECRET_KEY = "CNkx6KJYxTtEr6WF3ChOEKZIl4WMWG4n0i3Hvo8Ov4o";

    private static int MARKID = 0;

    public static boolean isStringNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    @Nullable
    private static String getMD5Hash(String original) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (md != null) {
                if (!isStringNullOrEmpty(original)) {
                    md.update(original.getBytes());
                }

                return convertToHex(md.digest());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String convertToHex(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder();
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buffer.append((char) ('0' + halfbyte));
                } else {
                    buffer.append((char) ('a' + (halfbyte - 10)));
                }

                halfbyte = aData & 0x0F;
            } while (two_halfs++ < 1);
        }

        return buffer.toString();
    }

    private static String getApiSignKey(String time) {
        StringBuffer buffer = new StringBuffer(SECRET_KEY.toString());
        buffer.append("ACCESS_KEY").append(ACCESS_KEY).append("TIMESTAMP").append(time).append(SECRET_KEY);
        return buffer.toString();
    }

    public static String getPostParams(String time) {
        String origin_sign = getApiSignKey(time);
        String sign_key = getMD5Hash(origin_sign);
        if (sign_key != null) {
            sign_key = sign_key.toUpperCase();
        }

        String data = "ACCESS_KEY=" + ACCESS_KEY + "&TIMESTAMP=" +
                time + "&SIGN_KEY=" + sign_key;

        return data;
    }

    public static String bitmapToBase64(Context context) {
        String result = null;
        AssetManager assetManager = context.getAssets();
        InputStream is = null;
        try {
            if (MARKID % 4 == 0) {
                is = assetManager.open("test4.jpg");
            } else if (MARKID % 4 == 1) {
                is = assetManager.open("test5.jpg");
            } else if (MARKID % 4 == 2) {
                is = assetManager.open("test6.jpg");
            } else if (MARKID % 4 == 3) {
                is = assetManager.open("test3.jpg");
            }

            MARKID++;
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(is);

        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static int getScreenWidth(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels;
    }

    public static int getScreenHeight(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.heightPixels;
    }
}
