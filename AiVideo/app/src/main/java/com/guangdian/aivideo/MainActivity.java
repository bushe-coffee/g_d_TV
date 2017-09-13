package com.guangdian.aivideo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ProgressBar mProgress;
    private ProgressBar mListProgress;
    private VideoView mVideoView;
    private RecyclerView mRecycleView;
    private RelativeLayout mAIContent;
    private String mUrl;

    private CommendListModel allModels;
    private List<CommendModel> mAllmModels;
    private List<CommendModel> mCurrentModels = new ArrayList<>();
    private AnalysisResultModel mAnalysisResultModel;
    private RecycleAdapter mAdapter;
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

    private boolean mIsPlaying = false;
    private int mVideoTotal = 0;
    private int mPlayPostion = 0;
    private MediaController mController;
    private boolean mIsShowRelate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (intent != null) {
            mUrl = intent.getStringExtra("video_url");
        }

        mVideoView = (VideoView) findViewById(R.id.main_video);
        mRecycleView = (RecyclerView) findViewById(R.id.main_relate);
        mAIContent = (RelativeLayout) findViewById(R.id.main_ai_content);

        int margin = YiPlusUtilities.getScreenHeight(this) / 6;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mAIContent.getLayoutParams();
        params.bottomMargin = margin;
        params.topMargin = margin;
        mAIContent.setLayoutParams(params);

        mProgress = (ProgressBar) findViewById(R.id.main_load_progress);
        mListProgress = (ProgressBar) findViewById(R.id.main_load_list_progress);
        mProgress.setVisibility(View.VISIBLE);
        mListProgress.setVisibility(View.GONE);

        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        initVideoView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestRelateData();

        if (mVideoView != null && !mIsPlaying && !mVideoView.isPlaying()) {
            if (mPlayPostion > 0) {
                mVideoView.start();
                mIsPlaying = true;
                mVideoView.seekTo(mPlayPostion);
                mPlayPostion = 0;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mIsPlaying && mVideoView.isPlaying()) {
            mPlayPostion = mVideoView.getCurrentPosition();
            mVideoView.stopPlayback();
            mIsPlaying = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.destroyDrawingCache();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mIsPlaying) {
                    mVideoView.pause();
                    mIsPlaying = false;
                } else {
                    mVideoView.start();
                    mIsPlaying = true;
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                analysisImage();
                break;
            case KeyEvent.META_SYM_ON:
                // 返回 键，
                if (mIsShowRelate) {
                    mIsShowRelate = false;
                    clearData();
                    mAdapter.notifyDataSetChanged();
                    mAIContent.setVisibility(View.GONE);
                    return true;
                } else {
                    this.finish();
                }
                break;
        }

        System.out.println("Yi plus   222222   " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    public void recycleClick(View view) {
        Toast.makeText(MainActivity.this, "hello  ", Toast.LENGTH_SHORT).show();
    }

    private class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleHolder> {

        private List<CommendModel> mDatas;

        void setDatas(List<CommendModel> datas) {
            mDatas.clear();
            mDatas.addAll(datas);
            this.notifyDataSetChanged();
        }

        RecycleAdapter() {
            mDatas = new ArrayList<>();
        }

        @Override
        public RecycleAdapter.RecycleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_content, parent, false);
            RecycleHolder holder = new RecycleHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(RecycleAdapter.RecycleHolder holder, int position) {
            if (holder != null && mDatas != null && position < mDatas.size()) {
                CommendModel model = mDatas.get(position);
                holder.mAdsContainer.setVisibility(View.GONE);
                holder.mContainer.setVisibility(View.GONE);
                if (model != null) {
                    if (!TAOBAO.equals(model.getData_source())) {
                        holder.mContainer.setVisibility(View.VISIBLE);
                        holder.mTitle.setText(model.getDetailed_title());
                        holder.mContent.setText(model.getDisplay_brief());
                        ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), holder.mImage);
                        if (BAIDU.equals(model.getData_source())) {
                            holder.mContainer.setBackgroundResource(R.drawable.baidu_bj);
                        } else if (DOUBAN.equals(model.getData_source())) {
                            holder.mContainer.setBackgroundResource(R.drawable.douban_bj);
                        } else if (WEIBO.equals(model.getData_source())) {
                            holder.mContainer.setBackgroundResource(R.drawable.weibo_bj);
                        } else if (VIDEO.equals(model.getData_source())) {
                            holder.mContainer.setBackgroundResource(R.drawable.video_dianbo_bj);
                        }
                    } else {
                        holder.mAdsContainer.setVisibility(View.VISIBLE);
                        ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), holder.mAds);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }

        class RecycleHolder extends RecyclerView.ViewHolder {

            private TextView mTitle;
            private TextView mContent;
            private ImageView mImage;
            private ImageView mAds;
            private LinearLayout mContainer;
            private LinearLayout mAdsContainer;
            private Button mBackGroundButton;

            RecycleHolder(View itemView) {
                super(itemView);
                mTitle = itemView.findViewById(R.id.item_content_container_title);
                mContent = itemView.findViewById(R.id.item_content_container_content);

                mImage = itemView.findViewById(R.id.item_content_container_image);
                mAds = itemView.findViewById(R.id.item_content_ads_image);
                mContainer = itemView.findViewById(R.id.item_content_container);
                mAdsContainer = itemView.findViewById(R.id.item_content_ads);

                mBackGroundButton = itemView.findViewById(R.id.item_background_button);
            }
        }
    }

    private void requestRelateData() {
        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);
        NetWorkUtils.post(YiPlusUtilities.VIDEO_COMMEND_URL, data, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    if (!YiPlusUtilities.isStringNullOrEmpty(res)) {
                        JSONArray array = new JSONArray(res);
                        allModels = new CommendListModel(array);
                        mAllmModels = allModels.getModels();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter = new RecycleAdapter();
                                mRecycleView.setAdapter(mAdapter);

                                analysisImage();
                            }
                        });

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initVideoView() {
        mVideoView.setVideoURI(Uri.parse(mUrl));

        // 监听视频装载完成的事件
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (mediaPlayer != null) {
                    mVideoTotal = mediaPlayer.getDuration();
                    mVideoView.start();
                    mIsPlaying = true;
                    mProgress.setVisibility(View.INVISIBLE);
                }
            }
        });

        // 监听播放发生错误时候的事件
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                return false;
            }
        });

        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                // 在有警告或错误信息时调用。例如：开始缓冲、缓冲结束、下载速度变化
                return false;
            }
        });

        // 监听播放完成的事件
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // TODO auto play next video
            }
        });
    }

    private void analysisImage() {

        // set ai content is visiable
        mAIContent.setVisibility(View.VISIBLE);
        mIsShowRelate = true;
        mListProgress.setVisibility(View.VISIBLE);

        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);

        String base64Image = YiPlusUtilities.bitmapToBase64(this);
        System.out.println("Yi Plus Image " + base64Image);
        String param = null;
        try {
            // base64 得到的 URL 在网络请求过程中 会出现 + 变 空格 的现象。 在 设置 base64 的字符串 之前 进行 格式化
            param = data + "&image=" + URLEncoder.encode(base64Image, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        NetWorkUtils.post(YiPlusUtilities.ANALYSIS_IMAGE_URL, param, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    System.out.println("Yi plus result  " +res);

                    JSONObject object = new JSONObject(res);
                    mAnalysisResultModel = new AnalysisResultModel(object);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SelectRightResult();
                        }
                    });

                } catch (Exception e) {
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
            mListProgress.setVisibility(View.GONE);
            mAdapter.setDatas(mCurrentModels);
        } else {
            getShowResultForAnalysis(false);
            mAdapter.setDatas(mCurrentModels);
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
        int bd=0, wb=0,db=0,dbsp=0,tb=0;
        if (!hasResult) {
            clearData();
            // 全部 随机
            Random baiduR = new Random();
            bd = baiduR.nextInt(allModels.getBaiduNum()-1);
            Random weiboR = new Random();
            wb = weiboR.nextInt(allModels.getWeiboNum()-1);
            Random doubanR = new Random();
            db = doubanR.nextInt(allModels.getDoubanNum()-1);
            Random dianboR = new Random();
            dbsp = dianboR.nextInt(allModels.getDianboNum()-1);
            Random taobaoR = new Random();
            tb = taobaoR.nextInt(allModels.getTaobaoNum()-1);
        }

        for (CommendModel model : mAllmModels) {
            if (model.getDetailed_image_url().length() < 8) {
                continue;
            }

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