package com.guangdian.dialog.adapters;


import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.guangdian.dialog.R;
import com.guangdian.dialog.models.CommendModel;
import com.guangdian.dialog.utils.KeyBoardCallback;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.RecycleHolder> {

    private List<CommendModel> mDatas;
    private Context mContext;
    private KeyBoardCallback mCallback;
    private List<Button> arrayButtons = new ArrayList<>();

    private static final String BAIDU = "百度百科";
    private static final String WEIBO = "微博";
    private static final String VIDEO = "点播视频";
    private static final String DOUBAN = "豆瓣";
    private static final String TAOBAO = "商品";

    public void setDatas(List<CommendModel> datas) {
        mDatas.clear();
        arrayButtons.clear();
        mDatas.addAll(datas);
        this.notifyDataSetChanged();
    }

    public RecycleAdapter(Context context) {
        mDatas = new ArrayList<>();
        this.mContext = context;
    }

    public void setKeyBoardCallBack(KeyBoardCallback callBack){
        mCallback = callBack;
    }

    @Override
    public RecycleAdapter.RecycleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_content, parent, false);

        return new RecycleAdapter.RecycleHolder(view);
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

                holder.mBackGroundButton.setTag(model.getTag_name());
                arrayButtons.add(holder.mBackGroundButton);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    private void clearButtonBackground() {
        for (Button button : arrayButtons) {
            button.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    class RecycleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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

            mBackGroundButton.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                    if (mCallback != null && keyCode == KeyEvent.KEYCODE_BACK) {
                        mCallback.onPressBack(keyCode);
                        return true;
                    }

                    clearButtonBackground();
                    view.setBackgroundColor(Color.BLUE);
                    return false;
                }
            });

            mBackGroundButton.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {
            Toast.makeText(mContext, (CharSequence) view.getTag(),Toast.LENGTH_SHORT).show();
        }
    }
}