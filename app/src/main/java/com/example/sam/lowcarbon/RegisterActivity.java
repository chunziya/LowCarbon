package com.example.sam.lowcarbon;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import com.mob.MobSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.smssdk.EventHandler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class
RegisterActivity extends AppCompatActivity implements RegisterFragment1.Listener, RegisterFragment2.Listener {

    private FragmentManager fm;
    private FragmentTransaction ft;

    private RegisterFragment1 registerFragment1;
    private RegisterFragment2 registerFragment2;

    private String telephone = "";
    private String username = "";
    private String qqid = "";
    private String wechatid = "";
    private int registerType = 0;

    private Intent mainIntent;

    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private EventHandler eventHandler;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initIntent();
        initSharePreferences();
        applyForPermission();

    }


    public void initIntent() {
        mainIntent = getIntent();
        registerType = mainIntent.getExtras().getInt("RegisterType");   //确定注册方式
        switch (registerType) {
            case Constant.REGISTER_PHONE:      //填写手机页面隐藏
                telephone = mainIntent.getExtras().getString("Telephone");
                fm = getSupportFragmentManager();
                ft = fm.beginTransaction();
                if (registerFragment2 == null) {
                    registerFragment2 = new RegisterFragment2();
                }
                ft.add(R.id.main_content, registerFragment2).commit();
                break;
            case Constant.REGISTER_QQ_PHONE:    //填写手机页面不隐藏
                qqid = mainIntent.getExtras().getString("QQid");      //如果是QQ注册则获取QQ的openid
                fm = getSupportFragmentManager();
                ft = fm.beginTransaction();
                if (registerFragment1 == null) {
                    registerFragment1 = new RegisterFragment1();
                }
                ft.add(R.id.main_content, registerFragment1).commit();
                break;
            case Constant.REGISTER_WECHAT_PHONE:
                break;
            default:
                break;
        }
    }

    @Override
    public void checkPhoneBindQQ(String tel) {    //检查该电话号码是否已经绑定QQ
        telephone = tel;
        JSONObject jsonObject = new JSONObject();
        String requestJson = "";
        try {
            jsonObject.put("telephone", telephone);   //获取电话号码
            jsonObject.put("qqid", mainIntent.getExtras().getString("QQid").trim());    //获取qq的openid
            requestJson = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        okHttpClient = new OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).connectTimeout(3, TimeUnit.SECONDS).build();
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        request = new Request.Builder().url(Constant.BASE_URL + "checkphonebindqq").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body().string();
                    JSONObject responseObject = new JSONObject(jsonString);
                    int result = responseObject.getInt("result");
                    switch (result) {
                        case -1:    //请求错误
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RegisterActivity.this, "请求数据失败,请稍后再试", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 0:     //手机号在数据库中且没有绑定
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RegisterActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);      //填完手机号直接跳转到主活动
                                }
                            });
                            break;
                        case 1:     //手机号不在数据库
                            registerType = Constant.REGISTER_QQ_PHONE;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { //页面跳转 填写用户名并设置头像
                                    Toast.makeText(RegisterActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();
                                    fm = getSupportFragmentManager();
                                    ft = fm.beginTransaction();
                                    if (registerFragment2 == null) {
                                        registerFragment2 = new RegisterFragment2();
                                    }
                                    ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                                    ft.add(R.id.main_content, registerFragment2).commit();
                                }
                            });
                            break;
                        case 2:     //手机号在数据库中且被绑定
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RegisterActivity.this, "该手机号已被绑定,请更换", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void submitRegister(String name) {  //提交申请
        username = name;   //获取用户名
        JSONObject jsonObject = new JSONObject();
        String requestJson = "";
        try {
            /* 下面三个为注册时必填写 */
            jsonObject.put("registertype", registerType);   //注册类型
            jsonObject.put("telephone", telephone); //电话号码
            jsonObject.put("username", username);   //用户名
            if (registerType == Constant.REGISTER_QQ_PHONE) {   //如果是QQ注册则放入openid
                jsonObject.put("qqid", qqid);
            }
            requestJson = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        File image = getImageFile();
        okHttpClient = new OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).connectTimeout(3, TimeUnit.SECONDS).build();
        requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("requestJson", requestJson)
                .addFormDataPart("userImage", "userImage.jpg", RequestBody.create(Constant.FILE, image))
                .build();
        request = new Request.Builder().url(Constant.BASE_URL + "register").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body().string();
                    JSONObject responseObject = new JSONObject(jsonString);
                    int result = responseObject.getInt("result");
                    if (result != 0) {  //注册成功
                        /* 数据保存在本地 */
                        editor.putString("telephone", telephone);
                        editor.putString("username", username);
                        editor.apply();

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);  //跳转到主活动
                        startActivity(intent);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "注册失败,请稍后再试", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public File getImageFile() {    //用户选择完头像后将头像保存到本地
        if (registerFragment2 == null) {
            registerFragment2 = new RegisterFragment2();
        }
        Bitmap bitmap = registerFragment2.getUserImageView();
        File dir = new File(Constant.LOCAL_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (dir.exists() && dir.canWrite()) {
            File file = new File(dir.getAbsolutePath() + "/" + telephone + ".jpg");     //创建文件
            Log.i("dirpath", dir.getAbsolutePath());
            if (!file.exists()) {
                Log.i("file", "not exsit");
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream out = new FileOutputStream(file);  //将bitmap读取到本地文件下
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Log.i("fileoutstream", "success");
                out.flush();
                out.close();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("dirs", "failed");
        }
        return null;
    }

    public void initSharePreferences() {    //初始化
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    @Override
    public void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, Constant.PHOTO_REQUEST_GALLERY);
    }

    /* 剪裁图片 */
    public void crop(Uri uri) {
        // 裁剪图片意图
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // 裁剪框的比例，1：1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);

        intent.putExtra("outputFormat", "JPEG");// 图片格式
        intent.putExtra("noFaceDetection", true);// 取消人脸识别
        intent.putExtra("return-data", true);
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CUT
        startActivityForResult(intent, Constant.PHOTO_REQUEST_CUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //活动回调
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.PHOTO_REQUEST_GALLERY:    //从相册回调
                if (data != null) {
                    Log.i("Gallery", "Success");
                    Uri uri = data.getData();   //获取选择图片的uri
                    crop(uri);  //剪裁图片
                }
                break;
            case Constant.PHOTO_REQUEST_CUT:    //剪裁回调
                if (data != null) {
                    Bitmap bitmap = data.getParcelableExtra("data");
                    if (registerFragment2 != null) {
                        registerFragment2.setUserImageView(bitmap);
                    }
                }
                break;
            default:
                break;
        }
    }


    public void applyForPermission() {
        if (Build.VERSION.SDK_INT >= 23) {  //SDK大于等于23
            Log.e("Version", ">23");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { //没有权限则申请权限
                Log.e("Permission", "No");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constant.SD_CODE);   //申请权限
            } else {
                Log.e("Permission", "Yes");
            }
        } else {    //SDK小于23则直接初始化定位
            Log.e("Version", "<23");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {   //请求回调
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.SD_CODE:  //申请SD卡权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { //请求通过
                    Log.i("grant", "success");
                } else {
                    Toast.makeText(RegisterActivity.this, "获取权限失败,请手动开启", Toast.LENGTH_SHORT).show();   //否则提醒用户手动开启
                }
                break;
            default:
                break;
        }
    }

}
