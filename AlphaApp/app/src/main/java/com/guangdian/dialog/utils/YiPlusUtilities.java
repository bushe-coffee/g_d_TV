package com.guangdian.dialog.utils;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;

import com.guangdian.dialog.CustomerService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

public class YiPlusUtilities {


    public static String LIST_URL = "http://47.94.37.237/api/video/all";
    public static String VIDEO_COMMEND_URL = "http://47.94.37.237/api/recommand";
    public static String ANALYSIS_IMAGE_URL = "http://47.94.37.237:62003/api/analysis";
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

    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                int size = 100; // 100 表示 不压缩
                bitmap.compress(Bitmap.CompressFormat.JPEG, size, baos);
                // 判断 图片 质量 大小 是否 小于 70k
                while (baos.toByteArray().length / 1024 > 70) {
                    baos.reset();
                    size -= 10;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, size, baos);
                }

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();

                // 保存 压缩后的图片
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
                saveImageTtoSd(BitmapFactory.decodeStream(inputStream, null, null), "main.jpg");

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

    // 长宽 等比例 缩放 。 并保存 不压缩 的 图片  名字 是 screenshot2
    public static String getBitmapFromSDCard(String path) {
        String result = "";
        if (!YiPlusUtilities.isStringNullOrEmpty(path)) {

            Bitmap bitmap = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);

                int h = options.outHeight;
                int w = options.outWidth;
                int hh = 640;
                int ww = 460;
                int be = w / ww; // 压缩率  按照 宽度 缩放 图片

                if (be <= 0) {
                    be = 1;
                }

                System.out.println("majie  tupian  yasuo  " + be);
                options.inSampleSize = be;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeFile(path, options);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int size = 100; // 100 表示 不压缩
                bitmap.compress(Bitmap.CompressFormat.JPEG, size, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();

                // 保存 压缩后的图片
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
                saveImageTtoSd(BitmapFactory.decodeStream(inputStream, null, null), "screenshot2.jpg");

                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            } catch (IOException e) {
                e.printStackTrace();
                result = "";
            }
        }

        return result;
    }

    //  直接 读取  路径 的图片
    public static String getBitmapBase64(String path) {
        String result = "";
        if (!YiPlusUtilities.isStringNullOrEmpty(path)) {
            try {
                Bitmap bitmap = null;
                BitmapFactory.Options options = new BitmapFactory.Options();

                bitmap = BitmapFactory.decodeFile(path, options);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int size = 100; // 100 表示 不压缩
                bitmap.compress(Bitmap.CompressFormat.JPEG, size, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();

                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }

                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            } catch (IOException e) {
                e.printStackTrace();
                result = "";
            }
        }

        return result;
    }

    // 缩放为 短边是 400 的 图片 并保存 名字是 thumbnail
    public static String getBitmapBase64Thumbnail(String path) {
        String result = "";
        if (!YiPlusUtilities.isStringNullOrEmpty(path)) {
            Bitmap bitmap = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                // 默认 是 ARGB_8888 每个 像素 是 4字节。 RGB_565 每个 像素 是 两字节
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeFile(path, options);

                int newW = 640;
                int newH = 400;
                int height = bitmap.getHeight();
                int width = bitmap.getWidth();
                float bilvH = ((newH * 1.0f) / (height * 1.0f));
//                newW = (int) (bilv * (width * 1.0f));
                float bilvW = ((newW * 1.0f) / (width * 1.0f));

                Matrix matrix = new Matrix();
                matrix.postScale(bilvW, bilvH);

                Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int size = 100; // 100 表示 不压缩
                bitmap1.compress(Bitmap.CompressFormat.JPEG, size, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();

                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }

                if (bitmap1 != null && !bitmap1.isRecycled()) {
                    bitmap1.recycle();
                    bitmap1 = null;
                }

                // 保存 压缩后的图片
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
                saveImageTtoSd(BitmapFactory.decodeStream(inputStream, null, null), "thumbnail.jpg");

                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
            } catch (IOException e) {
                e.printStackTrace();
                result = "";
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

    // save image
    public static void saveImageTtoSd(Bitmap bitmap, String name) {
        String cache = CustomerService.CACHE_PATH;
        File file = new File(cache + "/" + name);
        System.out.println("majie  tupian  save  ");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("majie   exception   " + e.getMessage());
        }

    }

    // 应用内截屏
    public static String screenShot(Activity activity) {
        String result = "";
        // 获取屏幕
        View dView = activity.getWindow().getDecorView().getRootView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        System.out.println("majie  screen  shot   ");
        Bitmap bmp = dView.getDrawingCache();
        if (bmp != null) {
            try {
                // 获取内置SD卡路径
                String sdCardPath = Environment.getExternalStorageDirectory().getPath();
                // 图片文件路径
                String filePath = sdCardPath + File.separator + "screenshot.jpg";
                result = filePath;

                System.out.println("Yi plus AI Video  " + filePath);

                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
            }
        }

        return result;
    }
}
