package com.example.sam.lowcarbon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.florent37.materialviewpager.header.MaterialViewPagerHeaderDecorator;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class FriendListFragment extends Fragment implements GeocodeSearch.OnGeocodeSearchListener {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private String telephone;

    private RecyclerView listView;
    private RefreshLayout list_refreshLayout;
    private FriendListAdapter friendListAdapter;
    private List<LatLonPoint> friendPosition;
    private GeocodeSearch geocodeSearch;
    private List<User> friendList;
    private LatLonPoint point;
    private String friendName;
    private boolean list_firstLoad = true;
    Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSharePreferences();
        telephone = getActivity().getIntent().getExtras().getString("Telephone").trim();

        friendList = new ArrayList<User>();
        listView = view.findViewById(R.id.list_recycleView);
        list_refreshLayout = view.findViewById(R.id.list_refresh_layout);
        listView.setHasFixedSize(true);   //设置滚动惯性平滑等
        listView.setNestedScrollingEnabled(false);
        list_refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshFriendList();
                refreshlayout.finishRefresh();
            }
        });
        refreshFriendList();

        geocodeSearch = new GeocodeSearch(getActivity());
        geocodeSearch.setOnGeocodeSearchListener(this);
    }

    public void initSharePreferences() {
        editor = getActivity().getSharedPreferences("LowCarbon", Context.MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getActivity().getSharedPreferences("LowCarbon", Context.MODE_PRIVATE);    //获取sharepreferences对象
    }

    private void refreshFriendList() {
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("telephone", telephone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String requestJson = jsonObject.toString();
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        request = new Request.Builder().url(Constant.BASE_URL + "friendlist").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    friendList.clear();
                    friendPosition = new ArrayList<LatLonPoint>();
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String username = jsonObject.getString("username");
                        final String tele = jsonObject.getString("telephone");
                        Double lat = jsonObject.getDouble("wei");
                        Double lon = jsonObject.getDouble("jing");
                        User user = new User(username, tele);
                        friendList.add(user);
                        friendPosition.add(new LatLonPoint(lat, lon));
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (list_firstLoad) {
                                    friendListAdapter = new FriendListAdapter(R.layout.friend_list_item, friendList);
                                    listView.setLayoutManager(new LinearLayoutManager(getContext()));
                                    listView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
                                    listView.setAdapter(friendListAdapter);
                                    list_firstLoad = false;
                                } else {
                                    friendListAdapter.notifyDataSetChanged();
                                }
                                friendListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                                        if (!sharedPreferences.getBoolean("shareposition", false)) {
                                            new AlertDialog.Builder(getContext())
                                                    .setMessage("开启位置共享服务才可以查看好友位置，是否前往设置？")
                                                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            startActivity(new Intent(getContext(), SettingsActivity.class));
                                                        }
                                                    })
                                                    .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                        }
                                                    })
                                                    .show();
                                        } else {
                                            point = friendPosition.get(position);
                                            if (point.getLatitude() == 0 && point.getLongitude() == 0) {
                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(getActivity(), "该好友尚未开启位置共享", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                friendName = friendList.get(position).getUsername();
                                                RegeocodeQuery query = new RegeocodeQuery(point, 1000, GeocodeSearch.AMAP);
                                                geocodeSearch.getFromLocationAsyn(query);
                                            }
                                        }
                                    }
                                });
                                friendListAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
                                    @Override
                                    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, final int position) {
                                        new MaterialDialog.Builder(getContext())
                                                .backgroundColorRes(R.color.white)
                                                .content("确定删除好友 " + friendList.get(position).getUsername() + " ？")
                                                .contentColorRes(R.color.black_semi_transparent)
                                                .negativeText("取消")
                                                .positiveText("确定")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        JSONObject jsonObject = new JSONObject();
                                                        try {
                                                            jsonObject.put("method", Constant.SOCKET_FRIEND_DELETE);
                                                            jsonObject.put("user1", telephone);
                                                            jsonObject.put("user2", friendList.get(position).getTelephone());
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
                                                        requestBody = new FormBody.Builder().add("requestJson", jsonObject.toString()).build();
                                                        request = new Request.Builder().url(Constant.BASE_URL + "addfriend").post(requestBody).build();
                                                        Call call = okHttpClient.newCall(request);
                                                        call.enqueue(new Callback() {
                                                            @Override
                                                            public void onFailure(Call call, IOException e) {
                                                                handler.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        Toast.makeText(getContext(), "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }

                                                            @Override
                                                            public void onResponse(Call call, Response response) throws IOException {
                                                                String jsonString = response.body().string();
                                                                try {
                                                                    JSONObject responseJson = new JSONObject(jsonString);
                                                                    int result = responseJson.getInt("result");
                                                                    if (result == 0) {
                                                                        handler.post(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                friendListAdapter.remove(position);
                                                                                Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                    }
                                                                } catch (JSONException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        });
                                                    }
                                                })
                                                .show();
                                        return false;
                                    }
                                });
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static FriendListFragment newInstance() {
        return new FriendListFragment();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int i) {
        if (i == 1000) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String address = result.getRegeocodeAddress().getFormatAddress();
                Toast.makeText(getActivity(), friendName + "最近一次运动在 " + address + " 附近", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
    }
}
