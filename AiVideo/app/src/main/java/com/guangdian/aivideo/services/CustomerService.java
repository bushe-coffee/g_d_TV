package com.guangdian.aivideo.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterViewFlipper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.guangdian.aivideo.NetWorkCallback;
import com.guangdian.aivideo.R;
import com.guangdian.aivideo.adapters.FilperAdapter;
import com.guangdian.aivideo.adapters.RecycleAdapter;
import com.guangdian.aivideo.models.AnalysisResultModel;
import com.guangdian.aivideo.models.CategoriesModel;
import com.guangdian.aivideo.models.CommendListModel;
import com.guangdian.aivideo.models.CommendModel;
import com.guangdian.aivideo.models.FacesModel;
import com.guangdian.aivideo.models.ScenesModel;
import com.guangdian.aivideo.utils.KeyBoardCallback;
import com.guangdian.aivideo.utils.NetWorkUtils;
import com.guangdian.aivideo.utils.YiPlusUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private ProgressBar mProgress;
    private String mBitmapBase64;
    private AdapterViewFlipper mFliper;

    private CommendListModel models;
    private RecycleAdapter mAdapter;
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

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                if (!YiPlusUtilities.isStringNullOrEmpty(mBitmapBase64)) {
                    analysisImage();
                } else {
                    SelectRightResult();
                }
            } else if (msg.arg1 == 2) {
                SelectRightResult();
            } else if (msg.arg1 == 3) {
                manager.removeViewImmediate(mContainerView);
            } else if (msg.arg1 == 4) {
                Bundle bundle = msg.getData();
                String source = bundle.getString("source");
                int arg2 = 0;
                if (BAIDU.equals(source)) {
                    arg2 = 0;
                } else if (VIDEO.equals(source)) {
                    arg2 = 1;
                } else if (WEIBO.equals(source)) {
                    arg2 = 2;
                } else if (DOUBAN.equals(source)) {
                    arg2 = 3;
                } else if (TAOBAO.equals(source)) {
                    arg2 = 4;
                }
                updateManagetView(models.getModels(arg2), source);
            }
        }
    };

    private KeyBoardCallback adapterCallBack = new KeyBoardCallback() {
        @Override
        public void onPressBack(int keyCode) {
            Message message = new Message();
            message.arg1 = 3;
            handler.sendMessage(message);
        }

        @Override
        public void onPressEnter(int keyCode, Bundle bundle) {
            Message message = new Message();
            message.setData(bundle);
            message.arg1 = 4;
            handler.sendMessage(message);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent();
        intent.setAction("com.yiplus.awake_server");
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null || YiPlusUtilities.isStringNullOrEmpty(intent.getStringExtra("ImagePath"))) {
            return START_STICKY;
        }

        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams params = setLayoutParams();
        View view = getViewForWindowToList();
        mContainerView = null;
        addViewToManager(view, params);

        handleIntent(intent);

        return START_STICKY;
    }

    private void addViewToManager(View view, WindowManager.LayoutParams params) {
        if (manager != null) {
            if (mContainerView != null) {
                manager.removeViewImmediate(mContainerView);
            }

            view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.animation_join));
            manager.addView(view, params);
            mContainerView = view;
        }
    }

    private WindowManager.LayoutParams setLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
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

    private void updateManagetView(List<CommendModel> datas, String source) {

        if (manager == null) {
            manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }
        WindowManager.LayoutParams params = setLayoutParams();
        View view = getViewForWindowToDetail(datas, source);

        addViewToManager(view, params);
    }

    private View getViewForWindowToDetail(List<CommendModel> datas, String source) {

        View view = LayoutInflater.from(this).inflate(R.layout.notifi_detail_layout, null, false);

        TextView title = view.findViewById(R.id.notifi_page_title);
        mFliper = view.findViewById(R.id.notifi_page_content);
        Button background = view.findViewById(R.id.notifi_page_background);
        Button backButton = view.findViewById(R.id.notifi_page_back_button);

        background.setOnKeyListener(keyListener);
        backButton.setOnKeyListener(keyListener);

        title.setText(source);
        FilperAdapter adapter = new FilperAdapter(this);
        adapter.setDatas(datas);
        mFliper.setAdapter(adapter);

        return view;
    }

    private OnKeyListener keyListener = new OnKeyListener() {
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
                        View view2 = getViewForWindowToList();
                        if (mAdapter != null) {
                            mAdapter.setDatas(mCurrentModels);
                        }

                        addViewToManager(view2, params);
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
                } else if (id == R.id.notifi_page_back_button) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

                        WindowManager.LayoutParams params = setLayoutParams();
                        View view2 = getViewForWindowToList();
                        if (mAdapter != null) {
                            mAdapter.setDatas(mCurrentModels);
                        }

                        addViewToManager(view2, params);
                        return false;
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

    private View getViewForWindowToList() {
        View view = LayoutInflater.from(this).inflate(R.layout.notification_layout, null, false);

        RecyclerView mRecycleView = view.findViewById(R.id.notifi_list);
        mProgress = view.findViewById(R.id.notifi_load_list_progress);

        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecycleAdapter(this);
        mRecycleView.setAdapter(mAdapter);

        mProgress.setVisibility(View.VISIBLE);

        // 网络不好的情况下 ，请求数据的时候 监听 返回键
        mRecycleView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Message message = new Message();
                    message.arg1 = 3;
                    handler.sendMessage(message);
                    return true;
                }

                return false;
            }
        });

        // 展示 数据的时候，监听button的返回键
        mAdapter.setKeyBoardCallBack(adapterCallBack);

        return view;
    }

    private void handleIntent(Intent intent) {
        String path = intent.getStringExtra("ImagePath");
        CACHE_PATH = this.getCacheDir().getAbsolutePath();
        mBitmapBase64 = "";
        if (!YiPlusUtilities.isStringNullOrEmpty(path)) {
            mBitmapBase64 = YiPlusUtilities.getBitmapBase64Thumbnail(path);
        }

        // TODO get the video all data
        this.ShowDefaultMessage2();
    }

    private void ShowDefaultMessage2() {
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

                        Message message = new Message();
                        message.arg1 = 1;
                        handler.sendMessage(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SelectRightResult() {
        clearData();

        // Show analysis result
        if (mAnalysisResultModel != null) {

            Map<String, List<String>> map = handleAnalysisResult();
            if (map == null || map.isEmpty()) {
                getShowResultForAnalysis(false);
                mAdapter.setDatas(mCurrentModels);
                mProgress.setVisibility(View.GONE);

                return;
            }

            // match analysis result and all data collections
            List<CommendModel> taobao = models.getModels(4);
            for (CommendModel m : taobao) {
                List<String> shang = map.get("商品");
                for (int j = 0; shang != null && j < shang.size(); ++j) {
                    if (shang.get(j).equals(m.getTag_name()) && mTaobao == 0) {
                        mCurrentModels.add(m);
                        mTaobao++;
                    }
                }
            }

            List<String> people = map.get("人物");
            List<CommendModel> baidu = models.getModels(0);
            List<CommendModel> weibo = models.getModels(2);
            List<CommendModel> douban = models.getModels(3);
            List<CommendModel> dianbo = models.getModels(1);
            // people 取第一个识别出来的 人
            String name = (people != null && people.size() > 0) ? people.get(0) : "";
            if (!YiPlusUtilities.isStringNullOrEmpty(name)) {
                for (CommendModel m : baidu) {
                    if (name.equals(m.getTag_name()) && mBaidu == 0) {
                        mCurrentModels.add(m);
                        mBaidu++;
                    }
                }

                for (CommendModel m : weibo) {
                    if (name.equals(m.getTag_name()) && mWeibo == 0) {
                        mCurrentModels.add(m);
                        mWeibo++;
                    }
                }

                for (CommendModel m : douban) {
                    if (name.equals(m.getTag_name()) && mDouban == 0) {
                        mCurrentModels.add(m);
                        mDouban++;
                    }
                }

                for (CommendModel m : dianbo) {
                    if (name.equals(m.getTag_name()) && mVideo == 0) {
                        mCurrentModels.add(m);
                        mVideo++;
                    }
                }
            }

            getShowResultForAnalysis(mCurrentModels.size() > 0);
            mAdapter.setDatas(mCurrentModels);
        } else {
            getShowResultForAnalysis(false);
            mAdapter.setDatas(mCurrentModels);
        }

        mProgress.setVisibility(View.GONE);
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
            int random = baiduR.nextInt(datas.size() - 1);
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

                    Message message = new Message();
                    message.arg1 = 2;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
