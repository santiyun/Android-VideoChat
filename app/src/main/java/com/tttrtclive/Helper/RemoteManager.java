package com.tttrtclive.Helper;

import com.tttrtclive.R;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.ui.MainActivity;

import java.util.ArrayList;

public class RemoteManager {

    private ArrayList<AudioRemoteWindow> mRemoteWindowList = new ArrayList();

    public RemoteManager(MainActivity mainActivity) {
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote1));
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote2));
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote3));
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote4));
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote5));
        mRemoteWindowList.add((AudioRemoteWindow) mainActivity.findViewById(R.id.remote6));
    }

    public void add(EnterUserInfo userInfo) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == -1) {
                audioRemoteWindow.show(userInfo);
                return;
            }
        }
    }

    public void remove(long id) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.hide();
                return;
            }
        }
    }

    public void muteAudio(long id, boolean mute) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.mute(mute);
                return;
            }
        }
    }

    public void updateAudioBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateAudioBitrate(bitrate);
                return;
            }
        }
    }

    public void updateVideoBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateVideoBitrate(bitrate);
                return;
            }
        }
    }

    public void updateSpeakState(long id, int volumeLevel) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            AudioRemoteWindow audioRemoteWindow = mRemoteWindowList.get(i);
            if (audioRemoteWindow.mId == id) {
                audioRemoteWindow.updateSpeakState(volumeLevel);
                return;
            }
        }
    }
}
