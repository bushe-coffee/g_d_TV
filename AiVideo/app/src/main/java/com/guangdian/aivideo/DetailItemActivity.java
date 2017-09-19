package com.guangdian.aivideo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.guangdian.aivideo.utils.YiPlusUtilities;


public class DetailItemActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setTextSize(150.0f);
        textView.setTextColor(Color.RED);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            String title = bundle.getString("title");
            if (!YiPlusUtilities.isStringNullOrEmpty(title)) {
                textView.setText(title);
            }
        }

        setContentView(textView);

    }
}
