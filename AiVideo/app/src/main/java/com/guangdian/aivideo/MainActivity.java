package com.guangdian.aivideo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ProgressBar mProgress;
    private VideoView mVideoView;
    private String mUrl;

    private CommendListModel allModels;
    private List<CommendModel> mAllmModels;
    private List<CommendModel> mCurrentModels = new ArrayList<>();
    private AnalysisResultModel mAnalysisResultModel;
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

        mProgress = (ProgressBar) findViewById(R.id.main_load_progress);
        mProgress.setVisibility(View.VISIBLE);

        initVideoView();
    }

    @Override
    protected void onResume() {
        super.onResume();

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

    private boolean doubleClick = false;
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
                if (doubleClick) {
                    Intent intent = new Intent("com.gw.cbn.screencap");
                    sendBroadcast(intent);
                }

                doubleClick = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleClick = false;
                    }
                }, 1000);

                break;
        }

        System.out.println("Yi plus   keyBoard    " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    public void recycleClick(View view) {
        Toast.makeText(MainActivity.this, "hello  ", Toast.LENGTH_SHORT).show();
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
                    mProgress.setVisibility(View.GONE);
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

}