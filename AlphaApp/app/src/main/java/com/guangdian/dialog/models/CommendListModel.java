package com.guangdian.dialog.models;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class CommendListModel {

    private final String BAIDU = "百度百科";
    private final String WEIBO = "微博";
    private final String VIDEO = "点播视频";
    private final String DOUBAN = "豆瓣";
    private final String TAOBAO = "商品";

    private List<CommendModel> models;
    private int BaiduNum = 0;
    private int WeiboNum = 0;
    private int DoubanNum = 0;
    private int DianboNum = 0;
    private int TaobaoNum = 0;

    public CommendListModel(JSONArray array) {
        BaiduNum = 0;
        WeiboNum = 0;
        DoubanNum = 0;
        DianboNum = 0;
        TaobaoNum = 0;

        if (array != null) {
            models = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                CommendModel commendModel = new CommendModel(array.optJSONObject(i));
                models.add(commendModel);
                if (BAIDU.equals(commendModel.getData_source())) {
                    BaiduNum++;
                }

                if (DOUBAN.equals(commendModel.getData_source())) {
                    DoubanNum++;
                }

                if (VIDEO.equals(commendModel.getData_source())) {
                    DianboNum++;
                }

                if (WEIBO.equals(commendModel.getData_source())) {
                    WeiboNum++;
                }

                if (TAOBAO.equals(commendModel.getData_source())) {
                    TaobaoNum++;
                }
            }
        }
    }

    public List<CommendModel> getModels() {
        return models;
    }

    public int getBaiduNum() {
        return BaiduNum;
    }

    public int getWeiboNum() {
        return WeiboNum;
    }

    public int getDoubanNum() {
        return DoubanNum;
    }

    public int getDianboNum() {
        return DianboNum;
    }

    public int getTaobaoNum() {
        return TaobaoNum;
    }
}
