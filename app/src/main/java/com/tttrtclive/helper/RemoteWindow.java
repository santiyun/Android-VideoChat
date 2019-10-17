package com.tttrtclive.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tttrtclive.R;
import com.tttrtclive.bean.EnterUserInfo;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

public class RemoteWindow extends RelativeLayout {

    private TTTRtcEngine mTTTEngine;
    private Context mContext;

    public long mId = -1;
    private boolean mIsMuted;

    private ImageView mSpeakImage;
    private TextView mIdView;
    private TextView mAudioBitrate;
    private TextView mVideoBitrate;
    private ViewGroup mVideoLayout;

    public RemoteWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View v = LayoutInflater.from(context).inflate(R.layout.audio_remote_window, this, true);
        mSpeakImage = v.findViewById(R.id.speakimage);
        mIdView = v.findViewById(R.id.id);
        mAudioBitrate = v.findViewById(R.id.audiorate);
        mVideoBitrate = v.findViewById(R.id.videorate);
        mVideoLayout = v.findViewById(R.id.video_layout);

        mTTTEngine = TTTRtcEngine.getInstance();
    }

    public void show(EnterUserInfo userInfo) {
        mId = userInfo.getId();
        mIdView.setText(String.valueOf(mId));
        mSpeakImage.setVisibility(View.VISIBLE);
        mIdView.setVisibility(View.VISIBLE);
        mAudioBitrate.setVisibility(View.VISIBLE);
        mVideoBitrate.setVisibility(View.VISIBLE);

        SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(mContext);
        mSurfaceView.setZOrderMediaOverlay(true);
        mTTTEngine.setupRemoteVideo(new VideoCanvas(mId, Constants.RENDER_MODE_HIDDEN, mSurfaceView));
        mVideoLayout.addView(mSurfaceView);
    }

    public void hide() {
        mId = -1;
        mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
        mSpeakImage.setVisibility(View.INVISIBLE);
        mIdView.setVisibility(View.INVISIBLE);
        mAudioBitrate.setVisibility(View.INVISIBLE);
        mVideoBitrate.setVisibility(View.INVISIBLE);
        mVideoLayout.removeAllViews();
    }

    public void mute(boolean mute) {
        mIsMuted = mute;
        mSpeakImage.setImageResource(mute ? R.drawable.jinyan : R.drawable.mainly_btn_speaker_selector);
    }

    public void updateAudioBitrate(String bitrate) {
        mAudioBitrate.setText(bitrate);
    }

    public void updateVideoBitrate(String bitrate) {
        mVideoBitrate.setText(bitrate);
    }

    public void updateSpeakState(int volumeLevel) {
        if (mIsMuted) return;

        if (volumeLevel >= 0 && volumeLevel <= 3) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
        } else if (volumeLevel > 3 && volumeLevel <= 6) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
        } else if (volumeLevel > 6 && volumeLevel <= 9) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
        }
    }

}
