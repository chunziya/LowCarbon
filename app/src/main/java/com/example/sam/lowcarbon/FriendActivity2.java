package com.example.sam.lowcarbon;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FriendActivity2 extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener,
        GeocodeSearch.OnGeocodeSearchListener {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private SocketService socketService;
    private ServiceConnection serviceConnection;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;

    private RefreshLayout list_refreshLayout;  //UI更新
    private RefreshLayout rank_refreshLayout;
    private RecyclerView friendListView;
    private RecyclerView friendRankView;
    private RecyclerView friendPhoneView;
    private FriendListAdapter friendListAdapter;
    private FriendRankAdapter friendRankAdapter;
    private FriendRecommendAdapter friendRecommendAdapter;
    private List<User> friendList;
    private List<User> friendPhone;
    private List<LatLonPoint> friendPosition;
    private List<UserRank> friendRank;
    private boolean list_firstLoad;
    private boolean rank_firstLoad;

    private SearchView searchView;
    private CircleImageView userImageView;
    private TextView userNameText;
    private String telephone;
    private String searchquery;

    private ViewPager viewPager;
    private ArrayList<View> listViews;
    private ImageView line;
    private TextView rank;
    private TextView list;
    private TextView search;
    private int offset = 0;   //移动条的偏移量
    private int currview = 0; //当前页面的编号
    private int lineWidth;    //移动条图片的长度
    private int one = 0;      //移动条滑动一页的距离
    private int two = 0;      //移动条滑动两页的距离

    private GeocodeSearch geocodeSearch;
    private LatLonPoint point;
    private String friendName;
    private View view1;
    private View view2;
    private View view3;
    private TextView tipText;
    private TextView blankView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);
        initSharePreferences();
        telephone = getIntent().getExtras().getString("Telephone").trim();

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        rank = (TextView) findViewById(R.id.rank);
        list = (TextView) findViewById(R.id.list);
        search = (TextView) findViewById(R.id.search);
        line = (ImageView) findViewById(R.id.img_cursor);

        //下划线动画的相关设置
        lineWidth = BitmapFactory.decodeResource(getResources(), R.drawable.line).getWidth();// 获取图片宽度
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;// 获取分辨率宽度
        offset = (screenW / 3 - lineWidth) / 2;// 计算偏移量
        Matrix matrix = new Matrix();
        matrix.postTranslate(offset, 0);
        line.setImageMatrix(matrix);// 设置动画初始位置
        one = offset * 2 + lineWidth;// 移动一页的偏移量,比如1->2,或者2->3
        two = one * 2;// 移动两页的偏移量,比如1直接跳3

        /* 申请权限 */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(0);
                    AndPermission.with(FriendActivity2.this)
                            .permission(Permission.ACCESS_COARSE_LOCATION,
                                    Permission.READ_CONTACTS)
                            .onGranted(new Action() {   //同意
                                @Override
                                public void onAction(List<String> permissions) {
                                }
                            })
                            .rationale(new Rationale() {   //拒绝一次
                                @Override
                                public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                                    new AlertDialog.Builder(FriendActivity2.this)
                                            .setMessage("您尚有权限仍未授取，是否继续授权？")
                                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    executor.execute();
                                                }
                                            })
                                            .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    executor.cancel();
                                                    finish();
                                                }
                                            })
                                            .show();
                                }
                            })
                            .onDenied(new Action() {   //禁止后不再询问
                                @Override
                                public void onAction(List<String> permissions) {
                                    if (AndPermission.hasAlwaysDeniedPermission(FriendActivity2.this, permissions)) {
                                        final SettingService service = AndPermission.permissionSetting(FriendActivity2.this);
                                        new AlertDialog.Builder(FriendActivity2.this)
                                                .setMessage("没有通讯录权限和位置权限应用程序将无法运行，是否去系统设置中授权？")
                                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        service.execute();
                                                    }
                                                })
                                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        service.cancel();
                                                        finish();
                                                    }
                                                })
                                                .show();
                                    } else {
                                        Toast.makeText(FriendActivity2.this, "您取消了授权", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            }).start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        initViews();
        //设置监听
        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
        rank.setOnClickListener(this);
        list.setOnClickListener(this);
        search.setOnClickListener(this);
        viewPager.addOnPageChangeListener(this);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                socketService = ((SocketService.MyBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(new Intent(FriendActivity2.this, SocketService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    public void initViews() {
        LayoutInflater mInflater = getLayoutInflater();

        //初始化排行界面
        view1 = mInflater.inflate(R.layout.friend_rank_view, null, false);
        rank_firstLoad = true;
        rank_refreshLayout = view1.findViewById(R.id.rank_refresh_layout);
        friendRankView = view1.findViewById(R.id.friend_rank);
        friendRankView.setHasFixedSize(true);   //设置滚动惯性平滑等
        friendRankView.setNestedScrollingEnabled(false);
        rank_refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshFriendRank();
                refreshlayout.finishRefresh();
            }
        });

        //初始化好友界面
        view2 = mInflater.inflate(R.layout.friend_list_view, null, false);
        list_firstLoad = true;
        list_refreshLayout = view2.findViewById(R.id.list_refresh_layout);
        friendListView = view2.findViewById(R.id.friend_list);
        friendListView.setHasFixedSize(true);   //设置滚动惯性平滑等
        friendListView.setNestedScrollingEnabled(false);
        list_refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshFriendList();
                refreshlayout.finishRefresh();
            }
        });

        //初始化查询界面
        view3 = mInflater.inflate(R.layout.friend_search_view, null, false);
        searchView = view3.findViewById(R.id.friend_search);
        blankView = view3.findViewById(R.id.blank_1);
        friendPhoneView = view3.findViewById(R.id.friend_phone);
        friendPhoneView.setHasFixedSize(true);   //设置滚动惯性平滑等
        friendPhoneView.setNestedScrollingEnabled(false);
//        searchView.setIconified(false);   //输入框可关闭
        searchView.onActionViewExpanded();   //输入框不可关闭
        try {   //去掉searchView中的下划线
            Field f = searchView.getClass().getDeclaredField("mSearchPlate");//通过反射，获得类对象的一个属性对象
            f.setAccessible(true);  //设置此私有属性是可访问的
            ((View) f.get(searchView)).setBackgroundResource(R.drawable.search_background);//获得属性值并设置此view的背景
        } catch (Exception e) {
            e.printStackTrace();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUser(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });

        refreshFriendRank();
        refreshFriendList();
        getContactsFriend();

        //往ViewPager填充View
        listViews = new ArrayList<View>();
        listViews.add(view1);
        listViews.add(view2);
        listViews.add(view3);
        FriendPagerAdapter pagerAdapter = new FriendPagerAdapter(listViews);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setCurrentItem(0);  //设置ViewPager当前页，从0开始算
    }

    public void refreshFriendList() {
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("telephone", telephone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String requestJson = jsonObject.toString();
        Log.e("friendList", requestJson);
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        request = new Request.Builder().url(Constant.BASE_URL + "friendlist").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        friendListAdapter.setEmptyView();
                        Toast.makeText(FriendActivity2.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    friendList = new ArrayList<User>();
                    friendPosition = new ArrayList<LatLonPoint>();
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String username = jsonObject.getString("username");
                        String telephone = jsonObject.getString("telephone");
                        Double lat = jsonObject.getDouble("wei");
                        Double lon = jsonObject.getDouble("jing");
                        User user = new User(username, telephone);
                        friendList.add(user);
                        friendPosition.add(new LatLonPoint(lat, lon));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (list_firstLoad) {
                                friendListAdapter = new FriendListAdapter(R.layout.friend_list_item, friendList);
                                friendListView.setLayoutManager(new LinearLayoutManager(FriendActivity2.this));
                                friendListView.addItemDecoration(new DividerItemDecoration(FriendActivity2.this, DividerItemDecoration.VERTICAL));
                                friendListView.setAdapter(friendListAdapter);
                                list_firstLoad = false;
                            } else {
                                friendListAdapter.notifyDataSetChanged();
                            }
                            //item点击事件
                            friendListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                                    if (!sharedPreferences.getBoolean("shareposition", false)) {
                                        new AlertDialog.Builder(FriendActivity2.this)
                                                .setMessage("开启位置共享服务才可以查看好友位置，是否前往设置？")
                                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        startActivity(new Intent(FriendActivity2.this, SettingsActivity.class));
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
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(FriendActivity2.this, "该好友尚未开启位置共享", Toast.LENGTH_SHORT).show();
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
                                    new MaterialDialog.Builder(FriendActivity2.this)
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
                                                            Toast.makeText(FriendActivity2.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void onResponse(Call call, Response response) throws IOException {
                                                            String jsonString = response.body().string();
                                                            try {
                                                                JSONObject responseJson = new JSONObject(jsonString);
                                                                int result = responseJson.getInt("result");
                                                                if (result == 0) {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            friendListAdapter.remove(position);
                                                                            rank_firstLoad = true;
                                                                            refreshFriendRank();
                                                                            Toast.makeText(FriendActivity2.this, "已删除", Toast.LENGTH_SHORT).show();
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

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        friendRankAdapter.setEmptyView();
                        Toast.makeText(FriendActivity2.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonString = response.body().string();
                Log.e("friendRank", jsonString);
                try {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    friendRank = new ArrayList<UserRank>();
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (rank_firstLoad) {
                                friendRankAdapter = new FriendRankAdapter(R.layout.friend_rank_item, friendRank);
                                friendRankView.setLayoutManager(new LinearLayoutManager(FriendActivity2.this));
                                friendRankView.addItemDecoration(new DividerItemDecoration(FriendActivity2.this, DividerItemDecoration.VERTICAL));
                                friendRankView.setAdapter(friendRankAdapter);
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

    public void searchUser(String query) {
        if (Constant.isChinaPhoneLegal(query)) {
            searchquery = query;
            JSONObject jsonObject = new JSONObject();
            String requestJson = null;
            try {
                jsonObject.put("user1", telephone);
                jsonObject.put("user2", searchquery);
                requestJson = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
            requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
            request = new Request.Builder().url(Constant.BASE_URL + "searchuser").post(requestBody).build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FriendActivity2.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonString = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(jsonString);
                        int result = responseJson.getInt("result");
                        switch (result) {
                            case 0:
                                final String username = responseJson.isNull("username") ? "null" : responseJson.getString("username");
                                final int relation = responseJson.getInt("relation");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String imageurl = Constant.IMAGE_URL + searchquery + ".jpg";
                                        if (relation == 1) {   //已经是好友或者搜索的是自己
                                            MaterialDialog dialog = new MaterialDialog.Builder(FriendActivity2.this)
                                                    .customView(R.layout.search_friend, false)
                                                    .show();
                                            View customView = dialog.getCustomView();
                                            userNameText = customView.findViewById(R.id.search_username);
                                            userImageView = customView.findViewById(R.id.search_imageview);
                                            tipText = customView.findViewById(R.id.tip);
                                            userNameText.setText(username);
                                            tipText.setText("你们已经是好友了～");
                                            GlideApp.with(FriendActivity2.this)
                                                    .load(imageurl)
                                                    .skipMemoryCache(true)
                                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                    .into(userImageView);
                                        } else if (telephone.equals(searchquery)) {
                                            Toast.makeText(FriendActivity2.this, "这是您自己～", Toast.LENGTH_SHORT).show();
                                        } else {
                                            MaterialDialog dialog = new MaterialDialog.Builder(FriendActivity2.this)
                                                    .customView(R.layout.search_friend, false)
                                                    .backgroundColorRes(R.color.white)
                                                    .negativeText("取消")
                                                    .positiveText("添加")
                                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                        @Override
                                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                            addFriend(telephone, searchquery);
                                                        }
                                                    })
                                                    .show();
                                            View customView = dialog.getCustomView();
                                            userNameText = customView.findViewById(R.id.search_username);
                                            userImageView = customView.findViewById(R.id.search_imageview);
                                            tipText = customView.findViewById(R.id.tip);
                                            userNameText.setText(username);
                                            tipText.setText("你们还不是好友，快添加好友吧～");
                                            GlideApp.with(FriendActivity2.this)
                                                    .load(imageurl)
                                                    .skipMemoryCache(true)
                                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                    .into(userImageView);
                                        }
                                    }

                                });

                                break;
                            case 1:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FriendActivity2.this, "用户不存在", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Toast.makeText(FriendActivity2.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
        }
    }

    public void getContactsFriend() {
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    .replace(" ", "").replace("+86", "");
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("phoneNum", phone);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("telephone", telephone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
        requestBody = new FormBody.Builder().add("requestJsonArray", jsonArray.toString()).add("requestJson", jsonObject.toString()).build();
        request = new Request.Builder().url(Constant.BASE_URL + "getcontacts").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        blankView.setVisibility(View.VISIBLE);
                        Toast.makeText(FriendActivity2.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseJson = response.body().string();
                try {
                    final JSONArray responseArray = new JSONArray(responseJson);
                    friendPhone = new ArrayList<User>();
                    for (int i = 0; i < responseArray.length(); i++) {
                        JSONObject jsonObject = responseArray.getJSONObject(i);
                        String userName = jsonObject.getString("username");
                        String telephone = jsonObject.getString("telephone");
                        User user = new User(userName, telephone);
                        friendPhone.add(user);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (responseArray.length() == 0) {
                                blankView.setVisibility(View.VISIBLE);
                            } else {
                                friendRecommendAdapter = new FriendRecommendAdapter(R.layout.friend_recommend_item, friendPhone);
                                friendPhoneView.setLayoutManager(new LinearLayoutManager(FriendActivity2.this));
                                friendPhoneView.setAdapter(friendRecommendAdapter);
                                friendRecommendAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                                    @Override
                                    public void onItemChildClick(BaseQuickAdapter adapter, final View view, final int position) {
                                        new MaterialDialog.Builder(FriendActivity2.this)
                                                .backgroundColorRes(R.color.white)
                                                .content("确定添加 " + friendPhone.get(position).getUsername() + " 为好友？")
                                                .contentColorRes(R.color.black_semi_transparent)
                                                .negativeText("取消")
                                                .positiveText("确定")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        addFriend(telephone, friendPhone.get(position).getTelephone());
                                                        Button button = view.findViewById(R.id.add_button);
                                                        button.setClickable(false);
                                                        button.setBackgroundColor(0);
                                                        button.setTextColor(Color.BLACK);
                                                        button.setText("已发送");
                                                    }
                                                })
                                                .show();
                                    }
                                });
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addFriend(String from, String to) {
        JSONObject object = new JSONObject();
        try {
            object.put("method", Constant.SOCKET_FRIEND_ADD);
            object.put("user", from);
            object.put("adduser", to);
            String msg = object.toString();
            Log.i("msg", msg);
            socketService.sendSocket(msg);
            Toast.makeText(FriendActivity2.this, "发送成功", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.clearFocus();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public void onPageSelected(int position) {
        Animation animation = null;
        switch (position) {
            case 0:
                if (currview == 1) {
                    animation = new TranslateAnimation(one, 0, 0, 0);
                } else if (currview == 2) {
                    animation = new TranslateAnimation(two, 0, 0, 0);
                }
                break;
            case 1:
                if (currview == 0) {
                    animation = new TranslateAnimation(offset, one, 0, 0);
                } else if (currview == 2) {
                    animation = new TranslateAnimation(two, one, 0, 0);
                }
                break;
            case 2:
                if (currview == 0) {
                    animation = new TranslateAnimation(offset, two, 0, 0);
                } else if (currview == 1) {
                    animation = new TranslateAnimation(one, two, 0, 0);
                }
                break;
        }
        currview = position;
        animation.setFillAfter(true);// true表示图片停在动画结束位置
        animation.setDuration(300); //设置动画时间为300毫秒
        line.startAnimation(animation);//开始动画
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rank:
                viewPager.setCurrentItem(0);
                break;
            case R.id.list:
                viewPager.setCurrentItem(1);
                break;
            case R.id.search:
                viewPager.setCurrentItem(2);
                break;
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int i) {
        if (i == 1000) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String address = result.getRegeocodeAddress().getFormatAddress();
                Toast.makeText(FriendActivity2.this, friendName + "最近一次运动在 " + address + " 附近", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

}
