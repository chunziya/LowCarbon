package com.example.sam.lowcarbon;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class FriendActivity extends AppCompatActivity implements FriendRecommendFragment.OnFragmentInteractionListener {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private MaterialViewPager mViewPager;
    private Toolbar toolbar;
    private SocketService socketService;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend2);
        mViewPager = (MaterialViewPager) findViewById(R.id.materialViewPager);

        mViewPager.getToolbar().setTitle("好友圈");
        toolbar = mViewPager.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setHomeButtonEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendActivity.super.onBackPressed();
            }
        });

        mViewPager.getViewPager().setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position % 3) {
                    case 0:
                        return FriendRankFragment.newInstance();
                    case 1:
                        return FriendListFragment.newInstance();
                    case 2:
                        return FriendRecommendFragment.newInstance();
                    default:
                        return FriendRankFragment.newInstance();
                }
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                switch (position % 3) {
                    case 0:
                        return "排行榜";
                    case 1:
                        return "好友列表";
                    case 2:
                        return "好友推荐";
                }
                return null;
            }
        });

        mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
            @Override
            public HeaderDesign getHeaderDesign(int page) {
                switch (page) {
                    case 0:
                        return HeaderDesign.fromColorResAndDrawable(R.color.green, getResources().getDrawable(R.drawable.m_background));
                    case 1:
                    case 2:
                    default:
                }
                //execute others actions if needed (ex : modify your header logo)
                return null;
            }
        });

        mViewPager.getViewPager().setOffscreenPageLimit(mViewPager.getViewPager().getAdapter().getCount());
        mViewPager.getPagerTitleStrip().setViewPager(mViewPager.getViewPager());

    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                socketService = ((SocketService.MyBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(new Intent(FriendActivity.this, SocketService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("FriendActivity2", "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    @Override
    public void addFriend(String from, String to) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", Constant.SOCKET_FRIEND_ADD);
            object.put("user", from);
            object.put("adduser", to);
            String msg = object.toString();
            Log.i("msg", msg);
            socketService.sendSocket(msg);
            Toast.makeText(FriendActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

