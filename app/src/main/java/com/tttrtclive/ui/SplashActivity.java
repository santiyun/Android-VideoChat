package com.tttrtclive.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclive.LocalConstans;
import com.tttrtclive.R;
import com.tttrtclive.bean.JniObjs;
import com.tttrtclive.bean.MyPermissionBean;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.helper.MyPermissionManager;
import com.tttrtclive.utils.SharedPreferencesUtil;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class SplashActivity extends BaseActivity {

    public static final int ACTIVITY_MAIN = 100;
    public static final int ACTIVITY_SETTING = 101;

    private ProgressDialog mDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private MyPermissionManager mMyPermissionManager;

    public boolean mIsLoging;
    private EditText mRoomIDET;
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
        if (!this.isTaskRoot()) {
            Intent mainIntent = getIntent();
            String action = mainIntent.getAction();
            if (action != null && mainIntent.hasCategory(Intent.CATEGORY_LAUNCHER) && action.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        ArrayList<MyPermissionBean> mPermissionList = new ArrayList<>();
        mPermissionList.add(new MyPermissionBean(Manifest.permission.WRITE_EXTERNAL_STORAGE, getResources().getString(R.string.permission_write_external_storage)));
        mPermissionList.add(new MyPermissionBean(Manifest.permission.RECORD_AUDIO, getResources().getString(R.string.permission_record_audio)));
        mPermissionList.add(new MyPermissionBean(Manifest.permission.CAMERA, getResources().getString(R.string.permission_camera)));
        mPermissionList.add(new MyPermissionBean(Manifest.permission.READ_PHONE_STATE, getResources().getString(R.string.permission_read_phone_state)));
        mMyPermissionManager = new MyPermissionManager(this, new MyPermissionManager.PermissionUtilsInter() {
            @Override
            public List<MyPermissionBean> getApplyPermissions() {
                return mPermissionList;
            }

            @Override
            public AlertDialog.Builder getTipAlertDialog() {
                return null;
            }

            @Override
            public Dialog getTipDialog() {
                return null;
            }

            @Override
            public AlertDialog.Builder getTipAppSettingAlertDialog() {
                return null;
            }

            @Override
            public Dialog getTipAppSettingDialog() {
                return null;
            }
        });
        boolean isOk = mMyPermissionManager.checkPermission();
        if (isOk) {
            init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyPermissionManager != null) {
            mMyPermissionManager.clearResource();
        }

        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mMyPermissionManager != null) {
            boolean isOk = mMyPermissionManager.onRequestPermissionsResults(this, requestCode, permissions, grantResults);
            if (isOk) {
                init();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MyPermissionManager.REQUEST_SETTING_CODE:
                if (mMyPermissionManager != null) {
                    boolean isOk = mMyPermissionManager.onActivityResults(requestCode);
                    if (isOk) {
                        init();
                    }
                }
                break;
            case ACTIVITY_MAIN:
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                break;
            case ACTIVITY_SETTING:
                if (data != null) {
                    mLocalVideoProfile = data.getIntExtra("LVP", mLocalVideoProfile);
                    mFRate = data.getIntExtra("FRATE", mFRate);
                    mBTate = data.getIntExtra("BRATE", mBTate);
                    mWidth = data.getIntExtra("WIDTH", mWidth);
                    mHeight = data.getIntExtra("HEIGHT", mHeight);
                    mUseHQAudio = data.getBooleanExtra("HQA", mUseHQAudio);
                }
                break;
        }
    }

    private void initView() {
        mRoomIDET = findViewById(R.id.room_id);
        TextView mVersion = findViewById(R.id.version);
        String string = getResources().getString(R.string.version_info);
        String result = String.format(string, TTTRtcEngine.getInstance().getSdkVersion());
        mVersion.setText(result);

        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setTitle("");
        mDialog.setMessage(getResources().getString(R.string.ttt_hint_loading_channel));
    }

    private void init() {
        // 初始化组件
        initView();
        // 读取保存的数据
        String roomID = (String) SharedPreferencesUtil.getParam(this, "RoomID", "");
        if (roomID != null) {
            mRoomIDET.setText(roomID);
            mRoomIDET.setSelection(roomID.length());
        }
        // 注册回调函数接收的广播
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        // 初始化 SDK
        initSDK();
    }

    private void initSDK() {
        // 创建 SDK 实例对象，请看 MainApplication 类。

        /*
         * 1.设置频道模式，SDK 默认就是 CHANNEL_PROFILE_COMMUNICATION(通信) 模式，但是 DEMO 显式的设置用于介绍接口。
         * 注意:该接口是全局接口，离开频道后状态不会清除，所以在模式需要发生变化时调用即可，无需每次加入频道都设置。
         */
        mTTTEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        /*
         * 2.设置角色身份，CHANNEL_PROFILE_COMMUNICATION 模式下可以设置两种角色
         * CLIENT_ROLE_BROADCASTER(副播) ：可以理解为麦上用户，默认可以说话。
         * CLIENT_ROLE_AUDIENCE(观众) ：可以理解为听众，默认只听不发。
         *
         * SDK 默认是 CLIENT_ROLE_BROADCASTER 角色，但是 DEMO 显式的设置用于介绍接口。
         * 注意:该接口是全局接口，离开频道后状态不会清除，所以在角色需要发生变化时调用即可，无需每次加入频道都设置。
         */
        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        // 3.启用视频功能模块
        mTTTEngine.enableVideo();
    }

    public void onClickEnterButton(View v) {
        mRoomName = mRoomIDET.getText().toString().trim();
        if (TextUtils.isEmpty(mRoomName)) {
            Toast.makeText(this, getResources().getString(R.string.ttt_error_enterchannel_check_channel_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.getTrimmedLength(mRoomName) > 19) {
            Toast.makeText(this, R.string.hint_channel_name_limit, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long roomId = Long.valueOf(mRoomName);
            if (roomId <= 0) {
                Toast.makeText(this, "房间号必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "房间号只支持整型字符串", Toast.LENGTH_SHORT).show();
        }

        if (mIsLoging) return;
        mIsLoging = true;

        Random mRandom = new Random();
        mUserId = mRandom.nextInt(999999);

        // 保存配置
        SharedPreferencesUtil.setParam(this, "RoomID", mRoomName);
        // 4.加入频道
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
        startActivityForResult(intent, ACTIVITY_SETTING);
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
                        startActivityForResult(activityIntent, ACTIVITY_MAIN);
                        mIsLoging = false;
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        int errorType = mJniObjs.mErrorType;
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
