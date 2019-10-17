package com.tttrtclive.bean;


/**
 * Created by root on 17-2-21.
 */

public class EnterUserInfo {

    private long id;

    public EnterUserInfo(long uid) {
        this.id = uid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
