package com.example.sam.lowcarbon;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * Created by abc on 2018/3/21.
 */

public class UserRank {
    private int rank;
    private int steps;
    private String username;
    private String telephone;
    private int iconId;

    public UserRank(int rank, int steps, String username, String telephone, int iconId) {
        this.rank = rank;
        this.steps = steps;
        this.username = username;
        this.telephone = telephone;
        this.iconId = iconId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
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

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }
}
