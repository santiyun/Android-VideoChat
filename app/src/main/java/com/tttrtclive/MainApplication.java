package com.tttrtclive;

import android.app.Application;

import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.io.File;

public class MainApplication extends Application {

    public MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        //1.设置SDK的回调接收类
        mMyTTTRtcEngineEventHandler = new MyTTTRtcEngineEventHandler(getApplicationContext());
        //2.创建SDK的实例对象
        TTTRtcEngine mTTTEngine = TTTRtcEngine.create(getApplicationContext(), <三体 APPID 的填写位置>, mMyTTTRtcEngineEventHandler);
        if (mTTTEngine == null) {
            System.exit(0);
            return;
        }

        //开启日志
        File fileDir = getExternalFilesDir(null);
        if (fileDir == null) {
            throw new RuntimeException("getExternalFilesDir is null!");
        }
        String logPath = fileDir + "/3TLog";
        TTTRtcEngine.getInstance().setLogFile(logPath);
    }
}
