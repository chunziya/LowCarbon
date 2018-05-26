package com.example.sam.lowcarbon;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRecommendAdapter extends BaseQuickAdapter<User, BaseViewHolder> {

    public FriendRecommendAdapter(int layoutResId, @Nullable List<User> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, User item) {
        helper.setText(R.id.friend_recommend_username, item.getUsername())
                .setText(R.id.friend_type, item.getTelephone())
                .addOnClickListener(R.id.add_button);

        String imageurl = Constant.IMAGE_URL + item.getTelephone() + ".jpg";
        GlideApp.with(mContext)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into((CircleImageView) helper.getView(R.id.friend_list_imageview));
    }
}
