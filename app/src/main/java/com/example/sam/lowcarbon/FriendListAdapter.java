package com.example.sam.lowcarbon;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by sam on 2017/12/3.
 */

public class FriendListAdapter extends BaseQuickAdapter<User, BaseViewHolder> {

    public FriendListAdapter(int layoutResId, @Nullable List<User> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, User item) {
        helper.setText(R.id.friend_list_username, item.getUsername())
                .setText(R.id.friend_telephone, item.getTelephone());

        String imageurl = Constant.IMAGE_URL + item.getTelephone() + ".jpg";
        GlideApp.with(mContext)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into((CircleImageView)helper.getView(R.id.friend_list_imageview));
    }
}