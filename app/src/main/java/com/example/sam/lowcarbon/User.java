package com.example.sam.lowcarbon;

import android.graphics.Bitmap;

/**
 * Created by sam on 2017/12/3.
 */

public class User {

    private String username;
    private String telephone;

    public User(String username, String telephone) {
        this.username = username;
        this.telephone=telephone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

}
