package com.guangdian.aivideo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.guangdian.aivideo.R;
import com.guangdian.aivideo.models.CommendModel;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;


public class FilperAdapter extends BaseAdapter {

    private Context mContext;
    private List<CommendModel> mDatas;

    public FilperAdapter(Context context) {
        this.mContext = context;
    }

    public void setDatas(List<CommendModel> datas) {
        this.mDatas = datas;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int i) {
        return mDatas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View parent, ViewGroup viewGroup) {
        ItemHolder holder;
        if (parent == null && mContext != null) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_filper_layout, viewGroup, false);
            holder = new ItemHolder(view);
            parent = view;
            parent.setTag(holder);
        } else {
            assert parent != null;
            holder = (ItemHolder) parent.getTag();
        }

        CommendModel model = mDatas.get(position);
        if (model != null) {
            holder.title.setText(model.getDisplay_title());
            holder.content.setText(model.getDetailed_description());
            if (model.getDetailed_image_url().length() > 8) {
                ImageLoader.getInstance().displayImage(model.getDetailed_image_url(), holder.image);
            }
        }

        return parent;
    }

    private class ItemHolder {
        TextView title;
        ImageView image;
        TextView content;

        ItemHolder(View view) {
            title = view.findViewById(R.id.item_filper_name);
            image = view.findViewById(R.id.item_filper_image);
            content = view.findViewById(R.id.item_filper_content);
        }
    }
}
