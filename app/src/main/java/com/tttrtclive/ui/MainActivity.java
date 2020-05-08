package com.tttrtclive.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclive.LocalConstans;
import com.tttrtclive.R;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.bean.JniObjs;
import com.tttrtclive.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.callback.PhoneListener;
import com.tttrtclive.dialog.ExitRoomDialog;
import com.tttrtclive.helper.RemoteManager;
import com.tttrtclive.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import androidx.annotation.Nullable;

public class MainActivity extends BaseActivity {

    private long mUserId;
    private String roomId;
    private boolean hqAudio;
    private int videoLevel, videoWidth, videoHeight, videoFps, videoBitrate;

    private TextView mAudioSpeedShow, mVideoSpeedShow;
    private ImageView mAudioChannel;
    private ViewGroup mLocalVideoLy;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog.Builder mErrorExitDialog;
    private ProgressDialog mJoinDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private boolean mIsMute = false;
    private boolean mIsHeadset;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker;
    private boolean mIsBackground;
    private boolean needResetLocalVideo;

    private RemoteManager mRemoteManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;

    public static int mCurrentAudioRoute;

    private SurfaceView mLocalVideoView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_videochat);
        Intent intent = getIntent();
        roomId = intent.getStringExtra("roomId");
        mUserId = intent.getLongExtra("uid", 0);
        hqAudio = intent.getBooleanExtra("audio_hq", false);
        videoLevel = intent.getIntExtra("videoLevel", Constants.TTTRTC_VIDEOPROFILE_DEFAULT);
        videoWidth = intent.getIntExtra("videoWidth", 360);
        videoHeight = intent.getIntExtra("videoHeight", 640);
        videoFps = intent.getIntExtra("videoFps", 15);
        videoBitrate = intent.getIntExtra("videoBitrate", 500);
        initView();
        initData();
        MyLog.d("MainActivity onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsBackground = false;
        if (needResetLocalVideo) {
            openLocalVideo();
            needResetLocalVideo = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsBackground = true;
    }


    @Override
    public void onBackPressed() {
        if (mExitRoomDialog != null) {
            mExitRoomDialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }

        if (mLocalBroadcast != null) {
            try {
                unregisterReceiver(mLocalBroadcast);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mTTTEngine.muteLocalAudioStream(false);
        super.onDestroy();
        MyLog.d("MainActivity onDestroy");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);
        mLocalVideoLy = findViewById(R.id.local_view_layout);

        String localChannelName = getString(R.string.ttt_prefix_channel_name) + ":" + roomId;
        ((TextView) findViewById(R.id.main_btn_title)).setText(localChannelName);
        ((TextView) findViewById(R.id.main_btn_host)).setText("ID：" + mUserId);

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

        mJoinDialog = new ProgressDialog(this);
        mJoinDialog.setCancelable(false);
        mJoinDialog.setTitle("");
        mJoinDialog.setMessage(getResources().getString(R.string.ttt_hint_loading_channel));
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initData() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);

        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        mRemoteManager = new RemoteManager(this);
        if (mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
        }

        // 启用视频模块功能
        mTTTEngine.enableVideo();
        // 打开本地视频
        openLocalVideo();
        // 避免阻塞UI
        // 开始加入频道
        new Thread(this::joinChannel).start();
    }

    private void openLocalVideo() {
        mLocalVideoView = TTTRtcEngine.CreateRendererView(this);
        mLocalVideoView.setZOrderMediaOverlay(false);
        mTTTEngine.enableVideo();
        mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mLocalVideoView), getRequestedOrientation());
        mTTTEngine.startPreview();
        runOnUiThread(() -> {
            mLocalVideoLy.addView(mLocalVideoView);
        });
    }

    private void closeLocalVideo() {
        runOnUiThread(() -> {
            mLocalVideoLy.removeAllViews();
        });
    }

    private void joinChannel() {
        mTTTEngine.enableMixAudioDataReport(true);
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
        // 4.设置视频编码属性，默认 SDK 使用 360P 质量等级。
        if (videoLevel != 0) {
            mTTTEngine.setVideoProfile(videoLevel, false);
        } else {
            mTTTEngine.setVideoProfile(videoWidth, videoHeight, videoFps, videoBitrate);
        }
        // 5.设置音频编码参数，SDK 默认为 ISAC 音频编码格式，32kbps 音频码率，适用于通话；高音质选用 AAC 格式编码，码率设置为96kbps。
        if (hqAudio) {
            mTTTEngine.setPreferAudioCodec(Constants.TTT_AUDIO_CODEC_AAC, 96, 1);
        } else {
            mTTTEngine.setPreferAudioCodec(Constants.TTT_AUDIO_CODEC_ISAC, 32, 1);
        }
        // 6.启用说话者音量提示，每300毫秒上报一次频道内所有用户的说话音量，每次上报一个用户，会触发多次。第二个参数平滑系数暂未实现。
        mTTTEngine.enableAudioVolumeIndication(300, 0);
        // 7.加入频道
        int join = mTTTEngine.joinChannel("", roomId, mUserId);
        if (join == 0) {
            runOnUiThread(() -> mJoinDialog.show());
        }
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room");
        mTTTEngine.leaveChannel();
        setResult(SplashActivity.ACTIVITY_MAIN);
        finish();
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-11-21 18:08:37<br/>
     * Description: 显示因错误的回调而退出的对话框
     *
     * @param message the message 错误的原因
     */
    public void showErrorExitDialog(String message) {
        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = (JniObjs) intent.getSerializableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                if (mJniObjs == null) {
                    return;
                }
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM: // 加入频道成功的信令
                        Toast.makeText(mContext, "加入频道成功", Toast.LENGTH_SHORT).show();
                        mJoinDialog.dismiss();
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR: // 接收加入频道失败的信令，或是sdk运行中出现的错误信令，需要手动调用leaveChannel
                        String errorMsg = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_format);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_TIMEOUT) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_timeout);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_VERIFY_FAILED) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_token_invaild);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_BAD_VERSION) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_version);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_CONNECT_FAILED) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_unconnect);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_NOEXIST) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_room_no_exist);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_SERVER_VERIFY_FAILED) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_verification_failed);
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_UNKNOW) {
                            errorMsg = mContext.getResources().getString(R.string.ttt_error_enterchannel_unknow);
                        }
                        mJoinDialog.dismiss();
                        showErrorExitDialog(errorMsg);
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_KICK: // 接收到服务器下发到异常退出信令，需要手动调用leaveChannel
                        String message = "";
                        int kickType = mJniObjs.mErrorType;
                        if (kickType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (kickType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (kickType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (kickType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (kickType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (kickType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (kickType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (kickType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (kickType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
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
                        String audioString = getResources().getString(R.string.ttt_audio_downspeed);
                        String audioResult = String.format(audioString, String.valueOf(mJniObjs.mAudioRecvBitrate));
                        mRemoteManager.updateAudioBitrate(mJniObjs.mUid, audioResult);
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        String videoString = getResources().getString(R.string.ttt_video_downspeed);
                        String videoResult = String.format(videoString, String.valueOf(mJniObjs.mVideoRecvBitrate));
                        mRemoteManager.updateVideoBitrate(mJniObjs.mUid, videoResult);
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_upspeed, String.valueOf(mJniObjs.mAudioSentBitrate));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        setTextViewContent(mVideoSpeedShow, R.string.ttt_video_upspeed, String.valueOf(mJniObjs.mVideoSentBitrate));
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
                        if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER || mAudioRoute == Constants.AUDIO_ROUTE_HEADPHONE) {
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

                        if (!mIsMute) {
                            mTTTEngine.muteLocalAudioStream(true);
                        }
                        mTTTEngine.muteAllRemoteAudioStreams(true);
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }

                            if (!mIsMute) {
                                mTTTEngine.muteLocalAudioStream(false);
                            }
                            mTTTEngine.muteAllRemoteAudioStreams(false);
                            mIsPhoneComing = false;
                        }
                        break;
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
                    case LocalConstans.CALL_BACK_ON_CAMERA_CONNECT_ERROR:
                        closeLocalVideo();
                        if (!mIsBackground) {
                            openLocalVideo();
                        } else {
                            needResetLocalVideo = true;
                        }
                        break;
                }
            }
        }
    }
}
