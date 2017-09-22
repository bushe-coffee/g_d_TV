package com.guangdian.aivideo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.guangdian.aivideo.fragments.VideoListFragment;
import com.guangdian.aivideo.models.VideoListModel;
import com.guangdian.aivideo.models.VideoModel;
import com.guangdian.aivideo.utils.NetWorkUtils;
import com.guangdian.aivideo.utils.YiPlusUtilities;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class ContentActivity extends FragmentActivity implements View.OnClickListener {

    private Button mButtonLast;
    private Button mButtonNext;
    private Button mButtonNum;
    private TextView mTextTotal;
    private ViewPager mViewPaper;
    private ProgressBar mLoading;

    private List<VideoModel> mListVideos;
    private int mCurrentPage = 1;
    private int mTotalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        mButtonLast = (Button) findViewById(R.id.content_page_last);
        mButtonNext = (Button) findViewById(R.id.content_page_next);
        mButtonNum = (Button) findViewById(R.id.content_page_num);
        mViewPaper = (ViewPager) findViewById(R.id.content_view_paper);
        mTextTotal = (TextView) findViewById(R.id.content_page_total);
        mLoading = (ProgressBar) findViewById(R.id.content_view_load_data);
        mLoading.setVisibility(View.VISIBLE);

        mButtonLast.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);
        mButtonNum.setText(mCurrentPage + "");

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mListVideos == null || mListVideos.size() == 0) {
            requestData();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.content_page_last:
                if (mCurrentPage > 1) {
                    mCurrentPage--;
                    mButtonNum.setText(mCurrentPage + "");
                    mViewPaper.setCurrentItem(mCurrentPage - 1);
                } else {
                    Toast.makeText(this, "已是第一页", Toast.LENGTH_SHORT).show();
                    Uri uri = Uri.parse("com.yiplus.result://authActivity");
                }

                break;
            case R.id.content_page_next:
                if (mCurrentPage < mTotalPages) {
                    mCurrentPage++;
                    mButtonNum.setText(mCurrentPage + "");
                    mViewPaper.setCurrentItem(mCurrentPage - 1);
                } else {
                    Toast.makeText(this, "已是最后一页", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    private void requestData(){
        String time = (System.currentTimeMillis() / 1000) + "";
        String data = YiPlusUtilities.getPostParams(time);
        System.out.println("Yi Plus  onResume  ");
        NetWorkUtils.post(YiPlusUtilities.LIST_URL, data, null, new NetWorkCallback() {
            @Override
            public void onServerResponse(Bundle result) {
                try {
                    String res = (String) result.get("result");
                    System.out.println("Yi Plus  list Result  " + res);
                    if (!YiPlusUtilities.isStringNullOrEmpty(res)) {
                        JSONArray array = new JSONArray(res);
                        VideoListModel model = new VideoListModel(array);
                        mListVideos = model.getVideoList();
                        if (mListVideos != null) {
                            mTotalPages = getPagesTotal(mListVideos.size());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextTotal.setText("共 " + mTotalPages + " 页");
                                    CustomerPagerAdapter pagerAdapter = new CustomerPagerAdapter(getSupportFragmentManager());
                                    mViewPaper.setAdapter(pagerAdapter);
                                    pagerAdapter.notifyDataSetChanged();
                                    System.out.println("Yi plus   " + "共 " + mTotalPages + " 页");
                                    mLoading.setVisibility(View.GONE);
                                }
                            });
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private int getPagesTotal(int videoSize) {
        if (videoSize > 0) {
            if (videoSize % 5 == 0) {
                return videoSize / 5;
            } else {
                return (videoSize / 5) + 1;
            }
        }

        return 0;
    }

    private class CustomerPagerAdapter extends FragmentStatePagerAdapter {

        CustomerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            VideoListFragment fragment = VideoListFragment.getInstance();

            int start = (mCurrentPage - 1) * 5;
            int last = (mCurrentPage * 5);
            if (last > mListVideos.size()) last = mListVideos.size();
            List<VideoModel> videoList = mListVideos.subList(start, last);
            fragment.setData(videoList);
            System.out.println("Yi plus   fragment   " + mListVideos.size());

            return fragment;
        }

        @Override
        public int getCount() {
            return mTotalPages;
        }
    }
}
