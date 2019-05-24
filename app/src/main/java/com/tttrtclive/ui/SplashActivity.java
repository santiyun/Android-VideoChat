package com.tttrtclive.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclive.LocalConstans;
import com.tttrtclive.R;
import com.tttrtclive.bean.JniObjs;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.utils.MyLog;
import com.tttrtclive.utils.SharedPreferencesUtil;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.yanzhenjie.permission.AndPermission;

import java.util.Random;

public class SplashActivity extends BaseActivity {

    private ProgressDialog mDialog;
    public static boolean mIsLoging;
    private EditText mRoomIDET;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private String mRoomName;
    private long mUserId;

    /*-------------------------------配置参数---------------------------------*/
    private int mLocalVideoProfile = Constants.TTTRTC_VIDEOPROFILE_DEFAULT;
    private boolean mUseHQAudio = false;
    private int mFRate;
    private int mBTate;
    private int mWidth, mHeight;
    /*-------------------------------配置参数---------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        // 权限申请
        AndPermission.with(this)
                .permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
                .start();
        init();
    }

    private void initView() {
        mRoomIDET = findViewById(R.id.room_id);
        TextView mVersion = findViewById(R.id.version);
        String string = getResources().getString(R.string.version_info);
        String result = String.format(string, TTTRtcEngine.getInstance().getSdkVersion());
        mVersion.setText(result);
    }

    private void init() {
        initView();
        // 读取保存的数据
        String roomID = (String) SharedPreferencesUtil.getParam(this, "RoomID", "");
        mRoomIDET.setText(roomID);
        mRoomIDET.setSelection(roomID.length());
        // 注册回调函数接收的广播
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setTitle("");
        mDialog.setMessage(getResources().getString(R.string.ttt_hint_loading_channel));
        MyLog.d("SplashActivity onCreate.... model : " + Build.MODEL);
        // 1.启用视频模块
        mTTTEngine.enableVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClickEnterButton(View v) {
        mRoomName = mRoomIDET.getText().toString().trim();
        if (TextUtils.isEmpty(mRoomName)) {
            Toast.makeText(this, getResources().getString(R.string.ttt_error_enterchannel_check_channel_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mIsLoging) return;
        mIsLoging = true;

        Random mRandom = new Random();
        mUserId = mRandom.nextInt(999999);

        // 保存配置
        SharedPreferencesUtil.setParam(this, "RoomID", mRoomName);

        mTTTEngine.joinChannel("", mRoomName, mUserId);
        mDialog.show();
    }

    public void onSetButtonClick(View v) {
        Intent intent = new Intent(this, SetActivity.class);
        intent.putExtra("LVP", mLocalVideoProfile);
        intent.putExtra("FRATE", mFRate);
        intent.putExtra("BRATE", mBTate);
        intent.putExtra("WIDTH", mWidth);
        intent.putExtra("HEIGHT", mHeight);
        intent.putExtra("HQA", mUseHQAudio);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mLocalVideoProfile = intent.getIntExtra("LVP", mLocalVideoProfile);
        mFRate = intent.getIntExtra("FRATE", mFRate);
        mBTate = intent.getIntExtra("BRATE", mBTate);
        mWidth = intent.getIntExtra("WIDTH", mWidth);
        mHeight = intent.getIntExtra("HEIGHT", mHeight);
        mUseHQAudio = intent.getBooleanExtra("HQA", mUseHQAudio);
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM:
                        //界面跳转
                        Intent activityIntent = new Intent();
                        activityIntent.putExtra("ROOM_ID", Long.parseLong(mRoomName));
                        activityIntent.putExtra("USER_ID", mUserId);
                        activityIntent.setClass(SplashActivity.this, MainActivity.class);
                        startActivity(activityIntent);
                        mDialog.dismiss();
                        mIsLoging = false;
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        int errorType = mJniObjs.mErrorType;
                        MyLog.d("onReceive CALL_BACK_ON_ERROR errorType : " + errorType);
                        if (errorType == Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_format), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_TIMEOUT) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_timeout), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_token_invaild), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_BAD_VERSION) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_version), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_CONNECT_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unconnect), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_NOEXIST) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_room_no_exist), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_SERVER_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_verification_failed), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_UNKNOW) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unknow), Toast.LENGTH_SHORT).show();
                        }
                        mIsLoging = false;
                        mDialog.dismiss();
                        break;
                }
            }
        }
    }

}
