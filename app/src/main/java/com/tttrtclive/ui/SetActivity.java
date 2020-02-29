package com.tttrtclive.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.tttrtclive.R;
import com.tttrtclive.bean.VideoProfileManager;
import com.wushuangtech.library.Constants;

import so.library.SoSpinner;

public class SetActivity extends BaseActivity implements SoSpinner.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    /*-------------------------------配置参数---------------------------------*/
    public int mLocalVideoProfile = Constants.TTTRTC_VIDEOPROFILE_DEFAULT;
    public boolean mUseHQAudio = false;
    private VideoProfileManager mVideoProfileManager = new VideoProfileManager();
    private EditText mPixView, mBiteView, mFrameView;
    private VideoProfileManager.VideoProfile mVideoProfile;
    private int mFRate;
    private int mBTate;
    private int mWidth, mHeight;
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
            mPixView.requestFocus();
            localPixSpinner.setSelectedIndex(mVideoProfileManager.mVideoProfiles.size());
        }

        ((Switch) findViewById(R.id.local_audio_switch)).setChecked(mUseHQAudio);
    }

    public void onExitButtonClick(View v) {
        exit();
    }

    @Override
    public void onClick(View v) {
        if (TextUtils.isEmpty(mPixView.getText())) {
            Toast.makeText(this, "自定义视频分辨率不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String resolutionStr = mPixView.getText().toString().trim();
        String[] wh = resolutionStr.split("x");
        if (wh.length != 2) {
            Toast.makeText(this, "自定义视频分辨率格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        int width, height, birrate, fps;
        try {
            width = Integer.parseInt(wh[0]);
            if (width <= 0) {
                Toast.makeText(this, "自定义视频分辨率宽必须大于0，输入正确参数", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "自定义视频分辨率格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            height = Integer.parseInt(wh[1]);
            if (height <= 0) {
                Toast.makeText(this, "自定义视频分辨率高必须大于0，输入正确参数", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "自定义视频分辨率格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        if (width * height > 1920 * 1080) {
            Toast.makeText(this, "自定义视频分辨率最大值为1920*1080", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(mBiteView.getText())) {
            Toast.makeText(this, "自定义视频码率不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            birrate = Integer.parseInt(mBiteView.getText().toString().trim());
            if (birrate <= 0) {
                Toast.makeText(this, "自定义视频码率必须大于0，输入正确参数", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "自定义视频码率格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        if (birrate > 5000) {
            Toast.makeText(this, "自定义视频码率最大值为5000kbps", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(mFrameView.getText())) {
            Toast.makeText(this, "自定义视频帧率不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fps = Integer.parseInt(mFrameView.getText().toString().trim());
            if (fps <= 0) {
                Toast.makeText(this, "自定义视频帧率必须大于0，输入正确参数", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "自定义视频帧率格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fps > 25) {
            Toast.makeText(this, "自定义视频帧率最大值为25", Toast.LENGTH_SHORT).show();
            return;
        }

        mWidth = width;
        mHeight = height;
        mBTate = Integer.parseInt(mBiteView.getText().toString().trim());
        mFRate = Integer.parseInt(mFrameView.getText().toString().trim());
        exit();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mUseHQAudio = isChecked;
    }

    @Override
    public void onItemSelected(View parent, int position) {
        if (position < mVideoProfileManager.mVideoProfiles.size()) {
            mVideoProfile = mVideoProfileManager.mVideoProfiles.get(position);
            mLocalVideoProfile = mVideoProfile.videoProfile;
            mPixView.setText(mVideoProfile.width + "x" + mVideoProfile.height);
            mBiteView.setText(mVideoProfile.bRate + "");
            mFrameView.setText(mVideoProfile.fRate + "");
            mPixView.requestFocus();
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
        setResult(SplashActivity.ACTIVITY_SETTING, intent);
        finish();
    }

}
