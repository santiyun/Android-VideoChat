package com.tttrtclive.ui;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import com.tttrtclive.Helper.RemoteManager;
import com.tttrtclive.LocalConstans;
import com.tttrtclive.MainApplication;
import com.tttrtclive.R;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.bean.JniObjs;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.callback.PhoneListener;
import com.tttrtclive.dialog.ExitRoomDialog;
import com.tttrtclive.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.model.VideoCanvas;

public class MainActivity extends BaseActivity {

    private long mUserId;

    private TextView mAudioSpeedShow;
    private TextView mVideoSpeedShow;
    private ImageView mAudioChannel;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog.Builder mErrorExitDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private boolean mIsMute = false;
    private boolean mIsHeadset;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker;

    private RemoteManager mRemoteManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;

    public static int mCurrentAudioRoute;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_videochat);
        initView();
        initData();
        initEngine();
        initDialog();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        MyLog.d("MainActivity onCreate");
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }
        unregisterReceiver(mLocalBroadcast);
        super.onDestroy();
        MyLog.d("MainActivity onDestroy");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);

        Intent intent = getIntent();
        long roomId = intent.getLongExtra("ROOM_ID", 0);
        mUserId = intent.getLongExtra("USER_ID", 0);
        ((TextView) findViewById(R.id.main_btn_title)).setText("房号：" + roomId);
        ((TextView) findViewById(R.id.main_btn_host)).setText("ID：" + mUserId);

        SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(this);
        mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mSurfaceView), getRequestedOrientation());
        ((ConstraintLayout)findViewById(R.id.local_view_layout)).addView(mSurfaceView);

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        mAudioChannel.setOnClickListener(v -> {
            mIsMute = !mIsMute;
            if (mIsHeadset)
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
            else
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
            mTTTEngine.muteLocalAudioStream(mIsMute);
        });

        findViewById(R.id.main_btn_switch_camera).setOnClickListener(v -> {
            mTTTEngine.switchCamera();
        });
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    private void initDialog() {
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });
        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());


        mErrorExitDialog = new AlertDialog.Builder(this)
                .setTitle("退出房间提示")//设置对话框标题
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {//确定按钮的响应事件
                    exitRoom();
                });
    }

    private void initData() {
        mRemoteManager = new RemoteManager(this);

        if (mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
        }
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room");
        mTTTEngine.leaveChannel();
        finish();
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_USER_KICK:
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_KICK... ");
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.error_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.error_rtmp);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.error_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.error_anchorexited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.error_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.error_otherenter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.error_noaudio);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.error_novideo);
                        }

                        if (!TextUtils.isEmpty(message)) {
                            mErrorExitDialog.setMessage("退出原因: " + message);//设置显示的内容
                            mErrorExitDialog.show();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        mErrorExitDialog.setMessage("退出原因: 房间网络断开");//设置显示的内容
                        mErrorExitDialog.show();
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        MyLog.d("UI onReceive CALL_BACK_ON_USER_JOIN... uid : " + uid);
                        EnterUserInfo userInfo = new EnterUserInfo(uid);
                        mRemoteManager.add(userInfo);
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        mRemoteManager.remove(offLineUserID);
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        String audioString = getResources().getString(R.string.videoly_audiodown);
                        String audioResult = String.format(audioString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                        mRemoteManager.updateAudioBitrate(mJniObjs.mRemoteAudioStats.getUid(), audioResult);
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        String videoString = getResources().getString(R.string.videoly_videodown);
                        String videoResult = String.format(videoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                        mRemoteManager.updateVideoBitrate(mJniObjs.mRemoteVideoStats.getUid(), videoResult);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        setTextViewContent(mAudioSpeedShow, R.string.main_audioup, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        setTextViewContent(mVideoSpeedShow, R.string.main_videoups, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_MUTE_AUDIO:
                        long muteUid = mJniObjs.mUid;
                        boolean mIsMuteAuido = mJniObjs.mIsDisableAudio;
                        MyLog.i("OnRemoteAudioMuted CALL_BACK_ON_MUTE_AUDIO start! .... " + mJniObjs.mUid
                                + " | mIsMuteAuido : " + mIsMuteAuido);
                        mRemoteManager.muteAudio(muteUid, mIsMuteAuido);
                        break;

                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE:
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER || mAudioRoute ==  Constants.AUDIO_ROUTE_HEADPHONE) {
                            mIsHeadset = false;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                        } else {
                            mIsHeadset = true;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        if (mIsMute) return;
                        int volumeLevel = mJniObjs.mAudioLevel;
                        if (mJniObjs.mUid == mUserId) {
                            if (mIsHeadset) {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_big_selector);
                                }
                            } else {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
                                }
                            }
                        } else {
                            mRemoteManager.updateSpeakState(mJniObjs.mUid, mJniObjs.mAudioLevel);
                        }
                        break;
                }
            }
        }
    }

}
