package com.guangdian.aivideo.models;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class CommendListModel {

    private List<CommendModel> models;

    public CommendListModel(JSONArray array) {
        if (array != null) {
            models = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                CommendModel commendModel = new CommendModel(array.optJSONObject(i));
                models.add(commendModel);
            }
        }
    }

    public List<CommendModel> getModels() {
        return models;
    }
}
