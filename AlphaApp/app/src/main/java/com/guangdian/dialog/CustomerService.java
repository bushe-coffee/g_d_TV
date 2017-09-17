package com.guangdian.dialog;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.guangdian.dialog.adapters.RecycleAdapter;
import com.guangdian.dialog.models.AnalysisResultModel;
import com.guangdian.dialog.models.CategoriesModel;
import com.guangdian.dialog.models.CommendListModel;
import com.guangdian.dialog.models.CommendModel;
import com.guangdian.dialog.models.FacesModel;
import com.guangdian.dialog.models.ScenesModel;
import com.guangdian.dialog.utils.KeyBoardCallback;
import com.guangdian.dialog.utils.NetWorkCallback;
import com.guangdian.dialog.utils.NetWorkUtils;
import com.guangdian.dialog.utils.YiPlusUtilities;

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

    private WindowManager manager;
    private View mContainerView;

    private RecyclerView mRecycleView;
    private ProgressBar mProgress;
    private String mBitmapBase64;

    private CommendListModel models;
    private List<CommendModel> mAllmModels;
    private RecycleAdapter mAdapter;
    private AnalysisResultModel mAnalysisResultModel;
    private List<CommendModel> mCurrentModels = new ArrayList<>();

    private int mBaidu = 0;
    private int mWeibo = 0;
    private int mVideo = 0;
    private int mDouban = 0;
    private int mTaobao = 0;
    private final String BAIDU = "百度百科";
    private final String WEIBO = "微博";
    private final String VIDEO = "点播视频";
    private final String DOUBAN = "豆瓣";
    private final String TAOBAO = "商品";

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                if (!YiPlusUtilities.isStringNullOrEmpty(mBitmapBase64)) {
                    System.out.println("majie   Bitmap   not null  " );
                    analysisImage();
                } else {
                    SelectRightResult();
                }
            } else if (msg.arg1 == 2) {
                SelectRightResult();
            } else if (msg.arg1 == 3) {
                System.out.println("majie  onKeyDown  ");
                manager.removeViewImmediate(mContainerView);
            }

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params;
        params = new WindowManager.LayoutParams();

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

        mContainerView = getViewForWindow();
        manager.addView(mContainerView, params);

        handleIntent(intent);

        mAdapter.setKeyBoardCallBack(new KeyBoardCallback() {
            @Override
            public void onPressBack(int keyCode) {
                System.out.println("majie  onKeyDown  " + keyCode);
                Message message = new Message();
                message.arg1 = 3;
                handler.sendMessage(message);
            }
        });

        return START_STICKY;
    }


    private View getViewForWindow() {
        View view = LayoutInflater.from(this).inflate(R.layout.notification_layout, null, false);

        mRecycleView = view.findViewById(R.id.notifi_relate);
        mProgress = view.findViewById(R.id.notifi_load_list_progress);

        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecycleAdapter(this);
        mRecycleView.setAdapter(mAdapter);

        mProgress.setVisibility(View.VISIBLE);

        return view;
    }

    private void handleIntent(Intent intent) {
        String path = intent.getStringExtra("ImagePath");
        mBitmapBase64 = "";
        if (!YiPlusUtilities.isStringNullOrEmpty(path)) {
            String before = YiPlusUtilities.getBitmapFromSDCard(path);
            String before2 = YiPlusUtilities.getBitmapBase64Thumbnail(path);
            path = "/storage/external_storage/thumbnail.jpg";

            mBitmapBase64 = YiPlusUtilities.getBitmapBase64(path);
            System.out.println("majie  " + mBitmapBase64);
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
                        mAllmModels = models.getModels();

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
            for (int i = 0; mAllmModels != null && i < mAllmModels.size(); ++i) {
                CommendModel model = mAllmModels.get(i);
                if ("商品".equals(model.getData_source())) {
                    List<String> shang = map.get("商品");
                    for (int j = 0; shang != null && j < shang.size(); ++j) {
                        if (shang.get(j).equals(model.getTag_name())) {
                            mCurrentModels.add(model);
                            mTaobao++;
                        }
                    }
                } else {
                    List<String> people = map.get("人物");
                    for (int j = 0; people != null && j < people.size(); ++j) {
                        if (people.get(j).equals(model.getTag_name())) {
                            mCurrentModels.add(model);
                            if (BAIDU.equals(model.getData_source())) {
                                mBaidu++;
                            }

                            if (DOUBAN.equals(model.getData_source())) {
                                mDouban++;
                            }

                            if (VIDEO.equals(model.getData_source())) {
                                mVideo++;
                            }

                            if (WEIBO.equals(model.getData_source())) {
                                mWeibo++;
                            }
                        }
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
                map.get("人物").add("陆毅");
            } else {
                List<String> person = new ArrayList<>();
                person.add(faces.getFace_attribute().get(i).getStar_name());
                map.put("人物", person);
            }
        }

        return map;
    }

    private void getShowResultForAnalysis(boolean hasResult) {
        int bd = 0, wb = 0, db = 0, dbsp = 0, tb = 0;
        if (!hasResult) {
            clearData();
            // 全部 随机
            Random baiduR = new Random();
            bd = baiduR.nextInt(models.getBaiduNum() - 1);
            Random weiboR = new Random();
            wb = weiboR.nextInt(models.getWeiboNum() - 1);
            Random doubanR = new Random();
            db = doubanR.nextInt(models.getDoubanNum() - 1);
            Random dianboR = new Random();
            dbsp = dianboR.nextInt(models.getDianboNum() - 1);
            Random taobaoR = new Random();
            tb = taobaoR.nextInt(models.getTaobaoNum() - 1);
        }

        for (CommendModel model : mAllmModels) {
//            if (model.getDetailed_image_url().length() < 8) {
//                continue;
//            }

            if (BAIDU.equals(model.getData_source())) {
                if (mBaidu == bd) {
                    mCurrentModels.add(model);
                }
                mBaidu++;
            }

            if (DOUBAN.equals(model.getData_source())) {
                if (mDouban == db) {
                    mCurrentModels.add(model);
                }
                mDouban++;
            }

            if (VIDEO.equals(model.getData_source())) {
                if (dbsp == mVideo) {
                    mCurrentModels.add(model);
                }
                mVideo++;
            }

            if (TAOBAO.equals(model.getData_source())) {
                if (mTaobao == tb) {
                    mCurrentModels.add(model);
                }
                mTaobao++;
            }

            if (WEIBO.equals(model.getData_source())) {
                if (mWeibo == wb) {
                    mCurrentModels.add(model);
                }
                mWeibo++;
            }
        }
    }

    private void analysisImage() {

        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);

//        String base64Image = YiPlusUtilities.bitmapToBase64(mBitmap);
//        if (mBitmap != null) {
//            mBitmap.recycle();
//            mBitmap = null;
//        }

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

//        mAnalysisResultModel = null;
        mBaidu = 0;
        mWeibo = 0;
        mVideo = 0;
        mDouban = 0;
        mTaobao = 0;
    }

}
