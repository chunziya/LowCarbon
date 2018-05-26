package com.example.sam.lowcarbon;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.IBinder;
import android.print.PrinterId;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.florent37.materialviewpager.header.MaterialViewPagerHeaderDecorator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ServiceConnection serviceConnection;
    private SocketService socketService;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;

    private CircleImageView userImageView;
    private SearchView searchView;
    private TextView userNameText;
    private ListView listView;
    private TextView tipText;
    private String searchquery;
    private String telephone;
    private List<String> phoneList;
    private ArrayAdapter<String> adapter;
    private boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initSharePreferences();
        telephone = sharedPreferences.getString("telephone", null);

        phoneList = new ArrayList<String>();
        listView = (ListView) findViewById(R.id.associate_list);
        searchView = (SearchView) findViewById(R.id.friend_search);
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
                if (Constant.isChinaPhoneLegal(query.trim().replace(" ", ""))) {
                    searchUser(query);
                    phoneList.clear();
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SearchActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().equals("") && newText.trim().length() > 1) {
                    JSONArray jsonArray = new JSONArray();
                    Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    while (cursor.moveToNext()) {
                        String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                .replace(" ", "").replace("+86", "");
                        //正则表达式匹配
                        if (Pattern.compile(newText.trim().replace(" ", "")).matcher(phone).lookingAt()) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("phoneNum", phone);
                                jsonArray.put(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("telephone", telephone);
                        jsonObject.put("method", Constant.FRIEND_SEARCH);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
                    requestBody = new FormBody.Builder()
                            .add("requestJsonArray", jsonArray.toString())
                            .add("requestJson", jsonObject.toString()).build();
                    request = new Request.Builder().url(Constant.BASE_URL + "getcontacts").post(requestBody).build();
                    Call call = okHttpClient.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseJson = response.body().string();
                            Log.e("phoneFriend", "success!");
                            try {
                                final JSONArray responseArray = new JSONArray(responseJson);
                                phoneList.clear();
                                for (int i = 0; i < responseArray.length(); i++) {
                                    JSONObject jsonObject = responseArray.getJSONObject(i);
                                    phoneList.add(jsonObject.getString("telephone"));
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (firstLoad) {
                                            adapter = new ArrayAdapter<String>(SearchActivity.this, R.layout.phone_list_item, phoneList);
                                            listView.setAdapter(adapter);
                                            firstLoad = false;
                                        } else {
                                            adapter.notifyDataSetChanged();
                                        }
                                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                searchView.setQuery(phoneList.get(position), false);
                                            }
                                        });
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    if (!firstLoad) {
                        phoneList.clear();
                        adapter.notifyDataSetChanged();
                    }
                }
                return false;
            }
        });

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                socketService = ((SocketService.MyBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(new Intent(SearchActivity.this, SocketService.class), serviceConnection, BIND_AUTO_CREATE);
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
                            Toast.makeText(SearchActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
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
                                            MaterialDialog dialog = new MaterialDialog.Builder(SearchActivity.this)
                                                    .customView(R.layout.search_friend, false)
                                                    .show();
                                            View customView = dialog.getCustomView();
                                            userNameText = customView.findViewById(R.id.search_username);
                                            userImageView = customView.findViewById(R.id.search_imageview);
                                            tipText = customView.findViewById(R.id.tip);
                                            userNameText.setText(username);
                                            tipText.setText("你们已经是好友了～");
                                            GlideApp.with(SearchActivity.this)
                                                    .load(imageurl)
                                                    .skipMemoryCache(true)
                                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                    .into(userImageView);
                                        } else if (telephone.equals(searchquery)) {
                                            Toast.makeText(SearchActivity.this, "这是您自己～", Toast.LENGTH_SHORT).show();
                                        } else {
                                            MaterialDialog dialog = new MaterialDialog.Builder(SearchActivity.this)
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
                                            GlideApp.with(SearchActivity.this)
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
                                        Toast.makeText(SearchActivity.this, "用户不存在", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(SearchActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
        }
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
            Toast.makeText(SearchActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
