package com.guangdian.aivideo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;


public class MainActivity extends AppCompatActivity {

    private ProgressBar mProgress;
    private VideoView mVideoView;
    private String mUrl;

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

        }

        System.out.println("Yi plus   keyBoard    " + keyCode);
        return super.onKeyDown(keyCode, event);
    }


    private void initVideoView() {
        mVideoView.setVideoURI(Uri.parse(mUrl));
        mVideoView.setMediaController(new MediaController(this));

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