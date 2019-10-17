package com.tttrtclive.helper;

import com.tttrtclive.R;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.ui.MainActivity;

import java.util.ArrayList;

public class RemoteManager {

    private ArrayList<RemoteWindow> mRemoteWindowList = new ArrayList<>();

    public RemoteManager(MainActivity mainActivity) {
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote1));
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote2));
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote3));
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote4));
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote5));
        mRemoteWindowList.add(mainActivity.findViewById(R.id.remote6));
    }

    public void add(EnterUserInfo userInfo) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == -1) {
                remoteWindow.show(userInfo);
                return;
            }
        }
    }

    public void remove(long id) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == id) {
                remoteWindow.hide();
                return;
            }
        }
    }

    public void muteAudio(long id, boolean mute) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == id) {
                remoteWindow.mute(mute);
                return;
            }
        }
    }

    public void updateAudioBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == id) {
                remoteWindow.updateAudioBitrate(bitrate);
                return;
            }
        }
    }

    public void updateVideoBitrate(long id, String bitrate) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == id) {
                remoteWindow.updateVideoBitrate(bitrate);
                return;
            }
        }
    }

    public void updateSpeakState(long id, int volumeLevel) {
        for (int i = 0; i < mRemoteWindowList.size(); i ++) {
            RemoteWindow remoteWindow = mRemoteWindowList.get(i);
            if (remoteWindow.mId == id) {
                remoteWindow.updateSpeakState(volumeLevel);
                return;
            }
        }
    }
}
