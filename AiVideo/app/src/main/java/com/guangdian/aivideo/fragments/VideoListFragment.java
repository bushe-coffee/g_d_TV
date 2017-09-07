package com.guangdian.aivideo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.guangdian.aivideo.MainActivity;
import com.guangdian.aivideo.R;
import com.guangdian.aivideo.models.VideoModel;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoListFragment extends Fragment {

    private RecyclerView mRecycleView;
    private CustomerAdapter mAdapter;
    private List<VideoModel> mDatas = new ArrayList<>();

    public static VideoListFragment getInstance() {
        return new VideoListFragment();
    }

    public void setData(List<VideoModel> videoList) {
        this.mDatas = videoList;
        if (mRecycleView != null && mAdapter != null && mDatas!= null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);
        mRecycleView = view.findViewById(R.id.content_list);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecycleView.setLayoutManager(manager);
        mRecycleView.setLayoutFrozen(false);
        mAdapter = new CustomerAdapter();
        mRecycleView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        return view;
    }

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.RecycleHolder> implements View.OnClickListener {

        @Override
        public RecycleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_video_list, parent, false);

            return new RecycleHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomerAdapter.RecycleHolder holder, int position) {
            if (holder != null && mDatas != null && position < mDatas.size() && !getActivity().isFinishing()) {
                VideoModel model = mDatas.get(position);
                if (model != null) {
                    holder.mTitle.setText(model.getVideo_name());

                    Date date = new Date(model.getCreate_time() * 1000);
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    holder.mTime.setText(DateFormat.getDateInstance().format(date).toString());

                    holder.mSize.setText(model.getVideo_size() + " M");

                    holder.mOpen.setTag(model.getVideo_url());
                    holder.mOpen.setOnClickListener(this);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }

        @Override
        public void onClick(View view) {
            if (view != null) {
                String url = (String) view.getTag();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("video_url", url);
                startActivity(intent);
            }
        }

        class RecycleHolder extends RecyclerView.ViewHolder {

            private TextView mTitle;
            private TextView mTime;
            private TextView mSize;
            private Button mOpen;

            RecycleHolder(View itemView) {
                super(itemView);
                mTitle = itemView.findViewById(R.id.item_video_title);
                mTime = itemView.findViewById(R.id.item_video_time);
                mSize = itemView.findViewById(R.id.item_video_size);
                mOpen = itemView.findViewById(R.id.item_video_button);
            }
        }
    }
}
