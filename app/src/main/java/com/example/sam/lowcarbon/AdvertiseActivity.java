package com.example.sam.lowcarbon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
//import android.os.Build;
//import android.os.Handler;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdvertiseActivity extends AppCompatActivity {  //广告页面

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private String telephone;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertise);

        initSharePreferences(); //初始化本地数据
        initData();
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(0);
//                    AndPermission.with(AdvertiseActivity.this)
//                            .permission(Permission.WRITE_EXTERNAL_STORAGE)
//                            .onGranted(new Action() {   //同意
//                                @Override
//                                public void onAction(List<String> permissions) {
//                                    initData();
//                                }
//                            })
//                            .rationale(new Rationale() {   //拒绝一次
//                                @Override
//                                public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
//                                    new AlertDialog.Builder(AdvertiseActivity.this)
//                                            .setMessage("存储权限仍未授取，是否继续授权？")
//                                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    executor.execute();
//                                                }
//                                            })
//                                            .setNegativeButton("否", new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    executor.cancel();
//                                                    finish();
//                                                }
//                                            })
//                                            .show();
//                                }
//                            })
//                            .onDenied(new Action() {   //禁止后不再询问
//                                @Override
//                                public void onAction(List<String> permissions) {
//                                    if (AndPermission.hasAlwaysDeniedPermission(AdvertiseActivity.this, permissions)) {
//                                        final SettingService service = AndPermission.permissionSetting(AdvertiseActivity.this);
//                                        new AlertDialog.Builder(AdvertiseActivity.this)
//                                                .setMessage("没有存储权限应用程序将无法运行，是否去系统设置中授权？")
//                                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
//                                                    @Override
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        service.execute();
//                                                    }
//                                                })
//                                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
//                                                    @Override
//                                                    public void onClick(DialogInterface dialog, int which) {
//                                                        service.cancel();
//                                                        finish();
//                                                    }
//                                                })
//                                                .show();
//                                    } else {
//                                        Toast.makeText(AdvertiseActivity.this, "您取消了授权", Toast.LENGTH_SHORT).show();
//                                        finish();
//                                    }
//                                }
//                            }).start();
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
    /* 初始化本地对象 */
    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    public void initData() {
        /* 判断登录状态 */
        telephone=sharedPreferences.getString("telephone","null");
        if (!sharedPreferences.getBoolean("login", false)) {     //未登录
            Log.i("login", "false");
            Intent intent = new Intent(AdvertiseActivity.this, LoginActivity.class);   //未登录,则跳转到登录界面
            startActivity(intent);  //跳转活动
        } else {     //已登录
            Log.i("login", "true");
            final JSONObject jsonObject = new JSONObject();
            String requestJson = "";
            try {
                if (telephone.equals("null")) {
                    Intent intent = new Intent(AdvertiseActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    jsonObject.put("telephone", telephone);
                    requestJson = jsonObject.toString();    //打包成json字符串
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)  //设置连接超时时间
                    .readTimeout(3, TimeUnit.SECONDS)  //设置读取超时时间
                    .build();
            requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
            Log.e("requestJson", requestJson);
            request = new Request.Builder().url(Constant.BASE_URL + "requestinformation").post(requestBody).build();    //去网络请求数据
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {   //请求失败
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {   //直接跳转 加载本地数据
                            Toast.makeText(AdvertiseActivity.this, "您的网络可能有点问题", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(AdvertiseActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {   //请求成功
                    String jsonString = response.body().string();
                    JSONObject responseObject = null;
                    try {
                        responseObject = new JSONObject(jsonString);
                        int result = responseObject.getInt("result");
                        switch (result) {
                            case 0: //数据库中存在
                                /* 获取用户数据 或为空则返回null */
                                String username = responseObject.isNull("username") ? "null" : responseObject.getString("username");
                                String birthday = responseObject.isNull("birthday") ? "null" : responseObject.getString("birthday");
                                String gender = responseObject.isNull("gender") ? "null" : responseObject.getString("gender");
                                String blood = responseObject.isNull("blood") ? "null" : responseObject.getString("blood");
                                int height = responseObject.isNull("height") ? -1 : responseObject.getInt("height");
                                int weight = responseObject.isNull("weight") ? -1 : responseObject.getInt("weight");

                                /* 用户信息存入本地 */
                                editor.putString("username", username);
                                editor.putString("birthday", birthday);
                                editor.putString("gender", gender);
                                editor.putString("blood", blood);
                                editor.putInt("height", height);
                                editor.putInt("weight", weight);
                                editor.apply();

                                //根据电话号码和网络图片id去下载图片到本地
                                downloadImage(telephone);

                                Toast.makeText(AdvertiseActivity.this, "No Wifi", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(AdvertiseActivity.this, MainActivity.class);
                                startActivity(intent);
                                break;
                            case 1: //数据库中不存在
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(AdvertiseActivity.this, "请求数据失败", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(AdvertiseActivity.this, LoginActivity.class);    //跳转到登录界面
                                        startActivity(intent);
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

        }
    }
    /* 从网络下载图片 */
    public void downloadImage(String telephone) {
        File dir = new File(Constant.LOCAL_PATH);   //获得存储路径
        if (!dir.exists()) {    //不存在则新建文件夹
            dir.mkdirs();
        }
        if (dir.exists() && dir.canWrite()) {
            final File file = new File(dir.getAbsolutePath(), "/" + telephone + ".jpg");    //将电话号作为本地图片名称
            okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
            String imageurl = Constant.IMAGE_URL + telephone + ".jpg";  //组成图片url
            request = new Request.Builder().url(imageurl).get().build();
            Call call = okHttpClient.newCall(request);    //异步请求
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {   //请求超时
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AdvertiseActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {   //请求成功
                    InputStream is = response.body().byteStream();      //获取字节
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {    //读入到本地文件
                        fileOutputStream.write(buf, 0, len);
                    }
                    fileOutputStream.flush();   //刷新
                    fileOutputStream.close();
                    is.close(); //关闭连接
                    Log.i("download", "success");
                }
            });
        }
    }

}
