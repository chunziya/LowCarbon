package com.example.sam.lowcarbon;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

public class FriendRankFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private String telephone;

    private RecyclerView rankView;
    private RefreshLayout rank_refreshLayout;
    private FriendRankAdapter friendRankAdapter;
    private List<UserRank> friendRank;
    private boolean rank_firstLoad = true;
    Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_rank, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        telephone = getActivity().getIntent().getExtras().getString("Telephone").trim();

        friendRank = new ArrayList<UserRank>();
        rank_refreshLayout = view.findViewById(R.id.rank_refresh_layout);
        rankView = view.findViewById(R.id.rank_recyclerView);
        rankView.setHasFixedSize(true);   //设置滚动惯性平滑等
        rankView.setNestedScrollingEnabled(false);
        rank_refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshFriendRank();
                refreshlayout.finishRefresh();
            }
        });
        refreshFriendRank();
    }

    public void initSharePreferences() {
        editor = getActivity().getSharedPreferences("LowCarbon", Context.MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getActivity().getSharedPreferences("LowCarbon", Context.MODE_PRIVATE);    //获取sharepreferences对象
    }

    private void refreshFriendRank() {
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("telephone", telephone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String requestJson = jsonObject.toString();
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        request = new Request.Builder().url(Constant.BASE_URL + "friendrank").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
//                        friendRankAdapter.setEmptyView();
                        Toast.makeText(getContext(), "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString = response.body().string();
                Log.e("friendRank", jsonString);
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    friendRank.clear();
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String username = jsonObject.getString("username");
                        String telephone = jsonObject.getString("telephone");
                        Integer steps = jsonObject.getInt("ranksteps");
                        UserRank userRank;
                        switch (i) {
                            case 0:
                                userRank = new UserRank(i + 1, steps, username, telephone, R.drawable.ic_rank_1);
                                break;
                            case 1:
                                userRank = new UserRank(i + 1, steps, username, telephone, R.drawable.ic_rank_2);
                                break;
                            case 2:
                                userRank = new UserRank(i + 1, steps, username, telephone, R.drawable.ic_rank_3);
                                break;
                            default:
                                userRank = new UserRank(i + 1, steps, username, telephone, 0);
                                break;
                        }
                        friendRank.add(userRank);
                    }
                    Log.e("getData", "Success!");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (rank_firstLoad) {
                                friendRankAdapter = new FriendRankAdapter(R.layout.friend_rank_item, friendRank);
                                rankView.setLayoutManager(new LinearLayoutManager(getContext()));
                                rankView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
                                rankView.setAdapter(friendRankAdapter);
                                rank_firstLoad = false;
                            } else {
                                friendRankAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public static FriendRankFragment newInstance() {
        return new FriendRankFragment();
    }

}
