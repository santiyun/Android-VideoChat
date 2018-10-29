package com.tttrtclive.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.tttrtclive.R;
import com.tttrtclive.bean.VideoProfileManager;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import so.library.SoSpinner;

public class SetActivity extends BaseActivity implements SoSpinner.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener{

    private VideoProfileManager mVideoProfileManager = new VideoProfileManager();
    private EditText mPixView, mBiteView, mFrameView;
    private VideoProfileManager.VideoProfile mVideoProfile;

    /*-------------------------------配置参数---------------------------------*/
    public int mLocalVideoProfile = Constants.VIDEO_PROFILE_DEFAULT;
    private int mFRate;
    private int mBTate;
    private int mWidth, mHeight;
    public boolean mUseHQAudio = false;
    /*-------------------------------配置参数---------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        Intent intent = getIntent();
        mLocalVideoProfile = intent.getIntExtra("LVP", mLocalVideoProfile);
        mFRate = intent.getIntExtra("FRATE", mFRate);
        mBTate = intent.getIntExtra("BRATE", mBTate);
        mWidth = intent.getIntExtra("WIDTH", mWidth);
        mHeight = intent.getIntExtra("HEIGHT", mHeight);
        mUseHQAudio = intent.getBooleanExtra("HQA", mUseHQAudio);

        mPixView = findViewById(R.id.local_pix_view);
        mBiteView = findViewById(R.id.local_bite_rate);
        mFrameView = findViewById(R.id.local_frame_rate);

        SoSpinner localPixSpinner = findViewById(R.id.local_pix_spinner);
        localPixSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.local_ok_button).setOnClickListener(this);
        ((Switch) findViewById(R.id.local_audio_switch)).setOnCheckedChangeListener(this);

        if (mLocalVideoProfile != 0) {
            mVideoProfile = mVideoProfileManager.getVideoProfile(mLocalVideoProfile);
            localPixSpinner.setSelectedIndex(mVideoProfileManager.mVideoProfiles.indexOf(mVideoProfile));
        } else {
            mPixView.setText(mWidth + "x" + mHeight);
            mBiteView.setText(mBTate + "");
            mFrameView.setText(mFRate + "");
            localPixSpinner.setSelectedIndex(5);
        }

        ((Switch) findViewById(R.id.local_audio_switch)).setChecked(mUseHQAudio);
    }

    public void onExitButtonClick(View v) {
        exit();
    }

    @Override
    public void onClick(View v) {
        if (mLocalVideoProfile != 0) {
            TTTRtcEngine.getInstance().setVideoProfile(mVideoProfile.videoProfile, true);
        } else {
            String[] wh = mPixView.getText().toString().trim().split("x");
            mWidth = Integer.parseInt(wh[0]);
            mHeight = Integer.parseInt(wh[1]);
            mFRate = Integer.parseInt(mFrameView.getText().toString().trim());
            mBTate = Integer.parseInt(mBiteView.getText().toString().trim());
            TTTRtcEngine.getInstance().setVideoProfile(mWidth, mHeight, mFRate, mBTate);
        }
        TTTRtcEngine.getInstance().setHighQualityAudioParameters(mUseHQAudio);
        exit();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mUseHQAudio = isChecked;
    }

    @Override
    public void onItemSelected(View parent, int position) {
        if (position != 5) {
            mVideoProfile = mVideoProfileManager.mVideoProfiles.get(position);
            mLocalVideoProfile = mVideoProfile.videoProfile;
            mPixView.setText(mVideoProfile.width + "x" + mVideoProfile.height);
            mBiteView.setText(mVideoProfile.bRate + "");
            mFrameView.setText(mVideoProfile.fRate + "");

            mPixView.setEnabled(false);
            mBiteView.setEnabled(false);
            mFrameView.setEnabled(false);
        } else {
            mLocalVideoProfile = 0;
            mPixView.setEnabled(true);
            mBiteView.setEnabled(true);
            mFrameView.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        exit();
        super.onBackPressed();
    }

    private void exit() {
        Intent intent = new Intent();
        intent.putExtra("LVP", mLocalVideoProfile);
        intent.putExtra("FRATE", mFRate);
        intent.putExtra("BRATE", mBTate);
        intent.putExtra("WIDTH", mWidth);
        intent.putExtra("HEIGHT", mHeight);
        intent.putExtra("HQA", mUseHQAudio);
        setResult(1, intent);
        finish();
    }

}
