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
 * Created by abc on 2018/3/21.
 */

public class FriendRankAdapter extends BaseQuickAdapter<UserRank, BaseViewHolder> {

    public FriendRankAdapter(int layoutResId, @Nullable List<UserRank> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, UserRank item) {
        helper.setText(R.id.rank_num, Integer.toString(item.getRank()))
                .setText(R.id.friend_rank_username, item.getUsername())
                .setText(R.id.rank_steps, Integer.toString(item.getSteps()))
                .setBackgroundRes(R.id.icon_rank, item.getIconId());

        String imageurl = Constant.IMAGE_URL + item.getTelephone() + ".jpg";
        GlideApp.with(mContext)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into((CircleImageView) helper.getView(R.id.friend_rank_imageview));
    }
}
