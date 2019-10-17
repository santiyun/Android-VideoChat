package com.tttrtclive.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import com.wushuangtech.wstechapi.TTTRtcEngine;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by wangzhiguo on 17/10/12.
 */

public class BaseActivity extends AppCompatActivity {

    protected TTTRtcEngine mTTTEngine;
    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏

        //获取上下文
        mContext = this;
        //获取SDK实例对象
        mTTTEngine = TTTRtcEngine.getInstance();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}
