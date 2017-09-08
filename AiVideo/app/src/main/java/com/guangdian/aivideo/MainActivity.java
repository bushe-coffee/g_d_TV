package com.guangdian.aivideo;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.guangdian.aivideo.models.CommendListModel;
import com.guangdian.aivideo.models.CommendModel;
import com.guangdian.aivideo.utils.NetWorkUtils;
import com.guangdian.aivideo.utils.YiPlusUtilities;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProgressBar mProgress;
    private VideoView mVideoView;
    private RecyclerView mRecycleView;
    private RelativeLayout mAIContent;
    private String mUrl;
    private CommendListModel mListModels;

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
        mProgress = (ProgressBar) findViewById(R.id.main_load_progress);
        mProgress.setVisibility(View.VISIBLE);

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


    private class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleHolder> implements View.OnClickListener {

        private List<CommendModel> mDatas;

        RecycleAdapter(List<CommendModel> models) {
            mDatas = models;
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
                    if (!model.getData_source().equals("商品")) {
                        holder.mContainer.setVisibility(View.VISIBLE);
                        holder.mTitle.setText(model.getDetailed_title());
                        holder.mContent.setText(model.getDisplay_brief());
                        ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), holder.mImage);
                    } else {
                        holder.mAdsContainer.setVisibility(View.VISIBLE);
                        ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), holder.mAds);
                    }

                    holder.mParentContainer.setTag(model.getCast());
                    holder.mParentContainer.setOnClickListener(this);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }

        @Override
        public void onClick(View view) {
            String cast = (String) view.getTag();

        }

        class RecycleHolder extends RecyclerView.ViewHolder {

            private TextView mTitle;
            private TextView mContent;
            private ImageButton mImage;
            private ImageView mAds;
            private LinearLayout mContainer;
            private LinearLayout mAdsContainer;
            private LinearLayout mParentContainer;

            public RecycleHolder(View itemView) {
                super(itemView);
                mTitle = itemView.findViewById(R.id.item_content_container_title);
                mContent = itemView.findViewById(R.id.item_content_container_content);

                mImage = itemView.findViewById(R.id.item_content_container_image);
                mAds = itemView.findViewById(R.id.item_content_ads_image);
                mContainer = itemView.findViewById(R.id.item_content_container);
                mAdsContainer = itemView.findViewById(R.id.item_content_ads);

                mParentContainer = itemView.findViewById(R.id.item_content);
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
                        mListModels = new CommendListModel(array);
                        RecycleAdapter adapter = new RecycleAdapter(mListModels.getModels());
                        mRecycleView.setAdapter(adapter);
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
        AssetManager assetManager = getAssets();
        InputStream is = null;
        try {
            is = assetManager.open("test3.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);

        String base64Image = YiPlusUtilities.bitmapToBase64(bitmap);
        System.out.println("Yi Plus " + base64Image);
//        String image = base64Image.replace("^data:image/[a-z]+;base64,/", "");
        String image = base64Image.replace("^data:image/[^;]*;base64,?", "");
        System.out.println("Yi Plus Image " + image);
        String param = data + "&image=" + image;
        NetWorkUtils.post(YiPlusUtilities.ANALYSIS_IMAGE_URL, param, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    System.out.println("Yi Plus Image result " + res);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAIContent.setVisibility(View.VISIBLE);
                            mIsShowRelate = true;

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}