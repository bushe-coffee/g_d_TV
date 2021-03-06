package com.guangdian.aivideo.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterViewFlipper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.guangdian.aivideo.NetWorkCallback;
import com.guangdian.aivideo.R;
import com.guangdian.aivideo.adapters.FilperAdapter;
import com.guangdian.aivideo.models.AnalysisResultModel;
import com.guangdian.aivideo.models.CategoriesModel;
import com.guangdian.aivideo.models.CommendListModel;
import com.guangdian.aivideo.models.CommendModel;
import com.guangdian.aivideo.models.FacesModel;
import com.guangdian.aivideo.models.ScenesModel;
import com.guangdian.aivideo.utils.NetWorkUtils;
import com.guangdian.aivideo.utils.YiPlusUtilities;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 在 这个 service 里面 添加 一个 View  类似 桌面 歌词
 */
public class CustomerService extends Service {

    public static String CACHE_PATH = "";

    private WindowManager manager;
    private View mContainerView;

    private String mBitmapBase64;
    private AdapterViewFlipper mFliper;

    private CommendListModel models;
    private AnalysisResultModel mAnalysisResultModel;
    private List<CommendModel> mCurrentModels = new ArrayList<>();

    private int mBaidu = 0;
    private int mWeibo = 0;
    private int mVideo = 0;
    private int mDouban = 0;
    private int mTaobao = 0;

    private static final String BAIDU = "百度百科";
    private static final String WEIBO = "微博";
    private static final String VIDEO = "点播视频";
    private static final String DOUBAN = "豆瓣";
    private static final String TAOBAO = "商品";

    private static boolean PREPARE_ALL_DATA = false;
    private static boolean PREPARE_IMAGE_BASE64 = false;
    private static boolean IS_SHOWING_WINDOW = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                if (!YiPlusUtilities.isStringNullOrEmpty(mBitmapBase64) && PREPARE_ALL_DATA && PREPARE_IMAGE_BASE64) {
                    PREPARE_IMAGE_BASE64 = false;
                    // 任何 图片都会有一个结果。只是返回的是不是 空
                    analysisImage();
                }
            } else if (msg.arg1 == 2) {
                // 识别 列表
                showAnsyncList();
                //  set right data
                SelectRightResult();
            } else if (msg.arg1 == 3) {
                // close the service
                manager.removeViewImmediate(mContainerView);
                IS_SHOWING_WINDOW = false;
                mContainerView = null;
            } else if (msg.arg1 == 4) {
                Bundle bundle = msg.getData();
                String source[] = bundle.getString("source").trim().split(" ");
                String type = source[0];
                String people = source[1];
                Log.d("Yi+", "淘宝   " + type + "   " + people);
                int arg2 = 0;
                if (BAIDU.equals(type)) {
                    arg2 = 0;
                } else if (VIDEO.equals(type)) {
                    arg2 = 1;
                } else if (WEIBO.equals(type)) {
                    arg2 = 2;
                } else if (DOUBAN.equals(type)) {
                    arg2 = 3;
                } else if (TAOBAO.equals(type)) {
                    arg2 = 4;
                }

                updateManagetView(models.getModels(arg2), type, people);
            }
        }
    };

    private OnKeyListener listKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (YiPlusUtilities.DOUBLECLICK) {
                synchronized (YiPlusUtilities.class) {
                    YiPlusUtilities.DOUBLECLICK = false;
                }

                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        sendMessageForHandle(3, null);
                        break;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        Bundle bundle = new Bundle();
                        bundle.putString("source", (String) view.getTag());
                        sendMessageForHandle(4, bundle);
                        break;
                }
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (YiPlusUtilities.class) {
                        YiPlusUtilities.DOUBLECLICK = true;
                    }
                }
            }, 1000);

            return false;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PREPARE_ALL_DATA = false;
        PREPARE_IMAGE_BASE64 = false;
        IS_SHOWING_WINDOW = false;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null || !intent.getBooleanExtra("StartScreenCap", false)) {
            return START_STICKY;
        }

        if (! IS_SHOWING_WINDOW) {
            mContainerView = null;
            PREPARE_ALL_DATA = false;
            PREPARE_IMAGE_BASE64 = false;
            IS_SHOWING_WINDOW = true;
            prepareAllData();
            showYiPlusLogo();
            screenCapAndRequest();
        }

        return START_STICKY;
    }

    private void prepareAllData() {
        // TODO get the video all data
        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);
        NetWorkUtils.post(YiPlusUtilities.VIDEO_COMMEND_URL, data, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    if (!YiPlusUtilities.isStringNullOrEmpty(res)) {
                        JSONArray array = new JSONArray(res);
                        models = new CommendListModel(array);
                        PREPARE_ALL_DATA = true;

                        sendMessageForHandle(1, null);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void screenCapAndRequest() {
        CACHE_PATH = this.getCacheDir().getAbsolutePath();
        try {
            Log.d("Yi+", "screencap start  " + System.currentTimeMillis());
            String adbContent = "/system/bin/screencap -p " + CACHE_PATH + "/screenshot.jpg";
            Process sh = Runtime.getRuntime().exec("su", null, null);
            OutputStream os = sh.getOutputStream();
            os.write(adbContent.getBytes("ASCII"));
            os.flush();
            os.close();
            sh.waitFor();
            Log.d("Yi+", "screencap end  " + System.currentTimeMillis());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            mBitmapBase64 = "";
            if (!YiPlusUtilities.isStringNullOrEmpty(CACHE_PATH + "/screenshot.jpg")) {
                mBitmapBase64 = YiPlusUtilities.getBitmapBase64Thumbnail(CACHE_PATH + "/screenshot.jpg");
                PREPARE_IMAGE_BASE64 = true;
                sendMessageForHandle(1, null);
            }
        }
    }

    private void sendMessageForHandle(int arg, Bundle bundle) {
        synchronized (handler) {
            Message message = new Message();
            message.arg1 = arg;
            if (bundle != null) {
                message.setData(bundle);
            }

            handler.sendMessage(message);
        }
    }

    private void showYiPlusLogo() {
        if (manager == null) {
            manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }

        WindowManager.LayoutParams params = setLayoutParams();
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        View view = getWelcomeViewPage();
        addViewToManager(view, params);
    }

    private View getWelcomeViewPage() {
        View view = LayoutInflater.from(this).inflate(R.layout.view_welcome_page, null, false);

        ImageButton image = view.findViewById(R.id.view_welcome_image);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.animation_scale_big);
        animation.setDuration(1000);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(5);
        image.startAnimation(animation);

        image.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Log.d("Yi+", " key Board  " + keyCode);
                    sendMessageForHandle(3, null);
                }
                return false;
            }
        });

        return view;
    }

    private void showAnsyncList() {
        if (manager == null) {
            manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }

        WindowManager.LayoutParams params = setLayoutParams();
        View view = getViewForWindowToList2();
        addViewToManager(view, params);
    }

    private void addViewToManager(View view, WindowManager.LayoutParams params) {
        if (manager != null) {
            if (mContainerView != null) {
                manager.removeViewImmediate(mContainerView);
                mContainerView = null;
            }

            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.animation_join));
            manager.addView(view, params);
            mContainerView = view;
        }
    }

    private WindowManager.LayoutParams setLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        params.width = metrics.widthPixels / 3;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

//        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        // 为了 可以 点击 其他的 应用 和 按钮 ，让他 失去焦点
//        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_SECURE;

        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.dimAmount = 0.4f;

        return params;
    }

    private void updateManagetView(List<CommendModel> datas, String source, String people) {
        if (manager == null) {
            manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }

        WindowManager.LayoutParams params = setLayoutParams();
        View view = getViewForWindowToDetail(datas, source, people);

        addViewToManager(view, params);
    }

    private View getViewForWindowToDetail(List<CommendModel> datas, String source, String people) {

        View view = LayoutInflater.from(this).inflate(R.layout.notifi_detail_layout, null, false);

        TextView title = view.findViewById(R.id.notifi_page_title);
        mFliper = view.findViewById(R.id.notifi_page_content);
        Button background = view.findViewById(R.id.notifi_page_background);

        background.setOnKeyListener(detailKeyListener);

        title.setText(source);
        FilperAdapter adapter = new FilperAdapter(this);
        if (TAOBAO.equals(source)) {
            List<CommendModel> showTb = new ArrayList<>();
            for (int j=0;j<datas.size(); ++j) {
                CommendModel tb = datas.get(j);
                if (people.equals(tb.getTag_name())) {
                    showTb.add(tb);
                }
            }

            Log.d("Yi+", "淘宝 商品  " + showTb.size());
            adapter.setDatas(showTb, 4);
        } else {
            for (int i =0; i < datas.size(); ++i) {
                CommendModel model = datas.get(i);
                if (people.equals(model.getDisplay_title())) {
                    if (BAIDU.equals(source)) {
                        datas.clear();
                        datas.add(model);
                    } else {
                        datas.remove(model);
                        datas.add(0, model);
                    }

                    break;
                }
            }

            adapter.setDatas(datas, 0);
        }

        mFliper.setAdapter(adapter);

        return view;
    }

    private OnKeyListener detailKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

            if (YiPlusUtilities.DOUBLECLICK) {
                System.out.println("majie  button    " + keyCode);
                synchronized (YiPlusUtilities.class) {
                    YiPlusUtilities.DOUBLECLICK = false;
                }

                int id = view.getId();
                if (id == R.id.notifi_page_background) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                        WindowManager.LayoutParams params = setLayoutParams();
                        View view2 = getViewForWindowToList2();
                        if (mCurrentModels != null) {
                            setListDatas(mCurrentModels);
                            addViewToManager(view2, params);
                        } else {
                            sendMessageForHandle(3, null);
                        }

                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (mFliper != null) {
                            mFliper.showNext();
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (mFliper != null) {
                            mFliper.showPrevious();
                        }
                        return true;
                    } else if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                        return true;
                    }
                }
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (YiPlusUtilities.class) {
                        YiPlusUtilities.DOUBLECLICK = true;
                    }
                }
            }, 1000);

            return false;
        }
    };

    private TextView baiduTitle;
    private TextView baiduContent;
    private ImageView baiduImage;
    private Button baiduBg;

    private TextView dianboTitle;
    private TextView dianboContent;
    private ImageView dianboImage;
    private Button dianboBg;

    private TextView weiboTitle;
    private TextView weiboContent;
//    private ImageView weiboImage;
    private Button weiboBg;

    private TextView doubanTitle;
    private TextView doubanContent;
//    private ImageView doubanImage;
    private Button doubanBg;

    private ImageView taobaoImage;
    private Button taobaoBg;


    private View getViewForWindowToList2() {
        View view = LayoutInflater.from(this).inflate(R.layout.view_ansync_list, null, false);
        baiduTitle = view.findViewById(R.id.view_baidu_title);
        baiduContent = view.findViewById(R.id.view_baidu_content);
        baiduImage = view.findViewById(R.id.view_baidu_image);
        baiduBg = view.findViewById(R.id.view_baidu_button);

        dianboTitle = view.findViewById(R.id.view_dianbo_title);
        dianboContent = view.findViewById(R.id.view_dianbo_content);
        dianboImage = view.findViewById(R.id.view_dianbo_image);
        dianboBg = view.findViewById(R.id.view_dianbo_button);

        weiboTitle = view.findViewById(R.id.view_weibo_title);
        weiboContent = view.findViewById(R.id.view_weibo_content);
//        weiboImage = view.findViewById(R.id.view_weibo_image);
        weiboBg = view.findViewById(R.id.view_weibo_button);

        doubanTitle = view.findViewById(R.id.view_douban_title);
        doubanContent = view.findViewById(R.id.view_douban_content);
//        doubanImage = view.findViewById(R.id.view_douban_image);
        doubanBg = view.findViewById(R.id.view_douban_button);

        taobaoImage = view.findViewById(R.id.view_taobao_image);
        taobaoBg = view.findViewById(R.id.view_taobao_button);

        baiduBg.setOnKeyListener(listKeyListener);
        dianboBg.setOnKeyListener(listKeyListener);
//        weiboBg.setOnKeyListener(listKeyListener);
//        doubanBg.setOnKeyListener(listKeyListener);
        taobaoBg.setOnKeyListener(listKeyListener);


        return view;
    }

    private void SelectRightResult() {
        clearData();

        // Show analysis result
        if (mAnalysisResultModel != null) {

            Map<String, List<String>> map = handleAnalysisResult();
            if (map == null || map.isEmpty()) {
                getShowResultForAnalysis(false);
                if (mCurrentModels != null) {
                    setListDatas(mCurrentModels);
                }

                return;
            }

            // match analysis result and all data collections
            List<String> people = map.get("人物");
            List<CommendModel> baidu = models.getModels(0);
            List<CommendModel> dianbo = models.getModels(1);
            List<CommendModel> weibo = models.getModels(2);
            List<CommendModel> douban = models.getModels(3);
            List<CommendModel> taobao = models.getModels(4);

            // people 取第一个识别出来的 人
            String name = (people != null && people.size() > 0) ? people.get(0) : "";
            Log.d("Yi+", "识别出来的 " + name);
            if (!YiPlusUtilities.isStringNullOrEmpty(name)) {
                for (CommendModel m : baidu) {
                    if (name.equals(m.getTag_name()) && mBaidu == 0) {
                        mCurrentModels.add(m);
                        mBaidu++;
                        break;
                    }
                }


                List<CommendModel> dianboPerson = new ArrayList<>();
                for (CommendModel m : dianbo) {
                    if (name.equals(m.getTag_name())) {
                        dianboPerson.add(m);
                    }
                }
                if (dianboPerson.size() > 0) {
                    randomOneData(dianboPerson);
                    Log.d("Yi+", "点播视频 数量 " + dianboPerson.size());
                    mVideo++;
                }


                List<CommendModel> weiboPerson = new ArrayList<>();
                for (CommendModel m : weibo) {
                    if (name.equals(m.getTag_name())) {
                        weiboPerson.add(m);
                    }
                }
                if (weiboPerson.size() > 0) {
                    randomOneData(weiboPerson);
                    mWeibo++;
                }


                List<CommendModel> taobaoPerson = new ArrayList<>();
                for (CommendModel m :taobao) {
                    if (name.equals(m.getTag_name())) {
                        taobaoPerson.add(m);
                    }
                }
                if (taobaoPerson.size() > 0) {
                    randomOneData(taobaoPerson);
                    Log.d("Yi+", "淘宝 商品 数量 " + taobaoPerson.size());
                    mTaobao++;
                }
            }

            //豆瓣没有 数据 ，添加
            getShowResultForAnalysis(mCurrentModels.size() > 0);
            if (mCurrentModels != null) {
                setListDatas(mCurrentModels);
            }
        } else {
            getShowResultForAnalysis(false);
            if (mCurrentModels != null) {
                setListDatas(mCurrentModels);
            }
        }
    }

    private Map<String, List<String>> handleAnalysisResult() {
        Map<String, List<String>> map = new HashMap<>();
        List<ScenesModel> sceneList = mAnalysisResultModel.getSceneList();
        List<CategoriesModel> categoriesList = mAnalysisResultModel.getCategoriesList();
        FacesModel faces = mAnalysisResultModel.getFaces();
        for (int i = 0; sceneList != null && i < sceneList.size(); ++i) {
            if (map.containsKey("商品")) {
                map.get("商品").add(sceneList.get(i).getScene_name());
            } else {
                List<String> taobao = new ArrayList<>();
                taobao.add(sceneList.get(i).getScene_name());
                map.put("商品", taobao);
            }
        }
        for (int i = 0; categoriesList != null && i < categoriesList.size(); ++i) {
            if (map.containsKey("商品")) {
                map.get("商品").add(categoriesList.get(i).getCategory_name());
            } else {
                List<String> taobao = new ArrayList<>();
                taobao.add(categoriesList.get(i).getCategory_name());
                map.put("商品", taobao);
            }
        }

        for (int i = 0; faces != null && i < faces.getFace_counts(); ++i) {
            if (map.containsKey("人物")) {
                map.get("人物").add(faces.getFace_attribute().get(i).getStar_name());
            } else {
                List<String> person = new ArrayList<>();
                person.add(faces.getFace_attribute().get(i).getStar_name());
                map.put("人物", person);
            }
        }

        return map;
    }

    private void getShowResultForAnalysis(boolean hasResult) {
        if (!hasResult) {
            clearData();
            // 全部 随机
            randomOneData(models.getModels(0));
            randomOneData(models.getModels(1));
            randomOneData(models.getModels(2));
            randomOneData(models.getModels(3));
            randomOneData(models.getModels(4));
        } else {
            if (mBaidu == 0) {
                randomOneData(models.getModels(0));
                mBaidu++;
            }
            if (mVideo == 0) {
                randomOneData(models.getModels(1));
                mVideo++;
            }
            if (mWeibo == 0) {
                randomOneData(models.getModels(2));
                mWeibo++;
            }
            if (mDouban == 0) {
                randomOneData(models.getModels(3));
                mDouban++;
            }
            if (mTaobao == 0) {
                randomOneData(models.getModels(4));
                mTaobao++;
            }
        }
    }

    private void randomOneData(List<CommendModel> datas) {
        Random baiduR = new Random();
        if (datas != null && datas.size() > 0) {
            int random = baiduR.nextInt(datas.size());
            mCurrentModels.add(datas.get(random));
        }

    }

    private void analysisImage() {

        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);

        String param = null;
        try {
            // base64 得到的 URL 在网络请求过程中 会出现 + 变 空格 的现象。 在 设置 base64 的字符串 之前 进行 格式化
            param = data + "&image=" + URLEncoder.encode(mBitmapBase64, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        NetWorkUtils.post(YiPlusUtilities.ANALYSIS_IMAGE_URL, param, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    System.out.println("majie  ANALYSIS_IMAGE_URL  " + res);
                    JSONObject object = new JSONObject(res);
                    mAnalysisResultModel = new AnalysisResultModel(object);

                    sendMessageForHandle(2, null);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setListDatas(List<CommendModel> listDatas) {
        if (listDatas != null && listDatas.size() > 0) {
            for (CommendModel model : listDatas) {
                if (BAIDU.equals(model.getData_source())) {
                    baiduTitle.setText(model.getDisplay_title());
                    baiduContent.setText(model.getDisplay_brief());
                    ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), baiduImage);
                    baiduBg.setTag(model.getData_source() + " " + model.getDisplay_title());
                }else if (VIDEO.equals(model.getData_source())) {
                    dianboTitle.setText(model.getDisplay_title());
                    dianboContent.setText(model.getDisplay_brief());
                    ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), dianboImage);
                    dianboBg.setTag(model.getData_source() + " " + model.getDisplay_title());
                } else if (WEIBO.equals(model.getData_source())) {
                    if (YiPlusUtilities.isStringNullOrEmpty(model.getDisplay_title()) || "null".equals(model.getDisplay_title())) {
                        weiboTitle.setVisibility(View.GONE);
                    } else {
                        weiboTitle.setText(model.getDisplay_title());
                    }

                    if (YiPlusUtilities.isStringNullOrEmpty(model.getDisplay_brief()) || "null".equals(model.getDisplay_brief())) {
                        weiboContent.setVisibility(View.GONE);
                    } else {
                        weiboContent.setText(model.getDisplay_brief());
                    }
//                    weiboBg.setTag(model.getData_source());
                } else if (DOUBAN.equals(model.getData_source())) {
                    if (YiPlusUtilities.isStringNullOrEmpty(model.getDisplay_title()) || "null".equals(model.getDisplay_title())) {
                        doubanTitle.setVisibility(View.GONE);
                    } else {
                        doubanTitle.setText(model.getDisplay_title());
                    }

                    if (YiPlusUtilities.isStringNullOrEmpty(model.getDisplay_brief()) || "null".equals(model.getDisplay_brief())) {
                        doubanContent.setVisibility(View.GONE);
                    } else {
                        doubanContent.setText(model.getDisplay_brief());
                    }

//                    doubanBg.setTag(model.getData_source());
                } else if (TAOBAO.equals(model.getData_source())) {
                    Log.d("Yi+", "image URL  " + model.getDetailed_image_url());
                    ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), taobaoImage);
                    taobaoBg.setTag(model.getData_source() + " " + model.getTag_name());
                }
            }
        }
    }

    private void clearData() {
        if (mCurrentModels != null) {
            mCurrentModels.clear();
        }

        mBaidu = 0;
        mWeibo = 0;
        mVideo = 0;
        mDouban = 0;
        mTaobao = 0;
    }
}
