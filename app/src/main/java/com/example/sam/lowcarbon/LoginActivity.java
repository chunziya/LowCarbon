package com.example.sam.lowcarbon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mob.MobSDK;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import okhttp3.*;


public class LoginActivity extends AppCompatActivity {

    /* QQ登录 */
    private Tencent mTencent;
    private IUiListener qqloginListener;

    /* UI控件 */
    private TextInputEditText telTextView;
    private TextInputEditText messagecodeTextView;
    private Button loginButton;
    private Button clearButton;
    private Button messageButton;
    private Button qqButton;

    /* http传输数据 */
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private EventHandler eventHandler;
    private Intent mainIntent;
    private CountDownTimer countDownTimer;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        telTextView = (TextInputEditText) findViewById(R.id.telephone_text);
        messagecodeTextView = (TextInputEditText) findViewById(R.id.code_text);
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (telTextView.getText().toString().trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "手机号不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    if (Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {
                        loginByPhone(telTextView.getText().toString().trim());
                    } else {
                        Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                    }

//                    if (Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {
//                        if (Constant.isCodeCorrest(messagecodeTextView.getText().toString().trim())) {
//                            SMSSDK.submitVerificationCode("86", telTextView.getText().toString().trim(),
//                                    messagecodeTextView.getText().toString().trim());    //提交短信验证码
//                        } else {
//                            Toast.makeText(LoginActivity.this, "验证码格式错误", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
//                    }
                }
            }
        });

        /* 清空键 */
        clearButton = (Button) findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                telTextView.setText("");
            }
        });

        /* 短信按钮用于发送短信验证码 */
        messageButton = (Button) findViewById(R.id.message_button);
        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {  //若手机号格式正确
                    SMSSDK.getVerificationCode("86", telTextView.getText().toString().trim());
                    messageButton.setClickable(false);
                    messageButton.getBackground().setAlpha(0);
                    countDownTimer = new CountDownTimer(60050, 1000) {  //60秒倒计时
                        @Override
                        public void onTick(long millisUntilFinished) {
                            messageButton.setText(millisUntilFinished / 1000 - 1 + "s");
                        }

                        @Override
                        public void onFinish() {
                            messageButton.setClickable(true);   //倒计时结束的操作
                            messageButton.setText("");
                            messageButton.getBackground().setAlpha(255);
                            messageButton.setBackgroundResource(R.drawable.ic_message);
                            Log.i("time", "finish");
                        }
                    };
                    countDownTimer.start();
                } else {    //否则
                    Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /* QQ按钮用于QQ登录 */
        qqButton = (Button) findViewById(R.id.qq_button);
        qqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTencent.login(LoginActivity.this, "all", qqloginListener); //跳转至QQ登录界面
            }
        });

        initQQData();   //初始化QQ登录的一些参数
        initMessage();  //初始化短信验证参数
        initSharePreferences();
    }

    /* 短信验证码功能初始化 */
    private void initMessage() {
        MobSDK.init(getApplicationContext(), Constant.MOB_APPKEY, Constant.MOB_APPSECRET);
        eventHandler = new EventHandler() {
            @Override
            public void afterEvent(int event, int result, Object data) {
                switch (event) {
                    case SMSSDK.EVENT_GET_VERIFICATION_CODE:    //获取验证码
                        if (result == SMSSDK.RESULT_COMPLETE) {    //回调完成
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "验证码已发送", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "验证码发送失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        break;
                    case SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE:    //验证验证码
                        if (result == SMSSDK.RESULT_COMPLETE) {    //回调完成
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "验证成功, 正在跳转页面", Toast.LENGTH_SHORT).show();
                                    loginByPhone(telTextView.getText().toString().trim());
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "验证码错误", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        SMSSDK.registerEventHandler(eventHandler);  //注册事件
    }

    /* QQ初始化 */
    private void initQQData() {
        mTencent = Tencent.createInstance(Constant.QQ_APPID, getApplicationContext());

        /* 监听登录 */
        qqloginListener = new IUiListener() {
            @Override
            public void onComplete(Object value) {
                if (value == null) {
                    Log.i("JSON", "Value is null");
                    Toast.makeText(LoginActivity.this, "未知错误", Toast.LENGTH_SHORT).show();
                }
                try {
                    JSONObject jo = (JSONObject) value;
                    int ret = jo.getInt("ret");
                    Log.i("JSON", String.valueOf(jo));
                    if (ret == 0) {
                        /* 若登录成功则获取相关数据 */
                        Toast.makeText(LoginActivity.this, "登陆成功", Toast.LENGTH_SHORT).show();
                        String openID = jo.getString("openid");
                        String accessToken = jo.getString("access_token");
                        String expires = jo.getString("expires_in");
                        mTencent.setOpenId(openID);     //给mTecent对象设置openid
                        mTencent.setAccessToken(accessToken, expires);
                        loginByQQ(openID);    //qq登录
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(UiError uiError) {
                Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "用户取消登录", Toast.LENGTH_SHORT).show();
            }
        };

    }

    /* 活动反馈 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* 识别请求码 */
        switch (requestCode) {
            case Constants.REQUEST_LOGIN:   //请求码为QQ授权
                Tencent.onActivityResultData(requestCode, resultCode, data, qqloginListener);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("LoginActivity", "OnDestory");
        SMSSDK.unregisterAllEventHandler();
    }

    @Override
    protected void onNewIntent(Intent newintent) {
        super.onNewIntent(newintent);   //获取新的intent
        Log.i("LoginActivity", "onNewIntent");
        setIntent(newintent);   //设置新的intent
        mainIntent = getIntent();   //获取新的intent
        if (mainIntent.getExtras().getInt("Code") == Constant.LOGOUT_CODE) {
            Toast.makeText(LoginActivity.this, "您已经退出登录", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {   //若在登录界面按回退则直接回到桌面
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    public void loginByPhone(String telephone) {    //手机登录
        JSONObject jsonObject = new JSONObject();
        String requestJson = "";
        try {
            jsonObject.put("loginmethod", Constant.PHONE_LOGIN);    //登录类型
            jsonObject.put("telephone", telephone);   //手机号
            requestJson = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();  //client对象
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        Log.i("requestJson", requestJson);
        request = new Request.Builder().url(Constant.BASE_URL + "login").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "网络连接异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body().string();
                    JSONObject responseObject = new JSONObject(jsonString);
                    int result = responseObject.getInt("result");
                    Intent intent = null;
                    switch (result) {
                        case 0:      //数据库中存在此号码
                            /* 获取用户信息 */
                            String telephone = telTextView.getText().toString();
                            String username = responseObject.isNull("username") ? "null" : responseObject.getString("username");
                            String birthday = responseObject.isNull("birthday") ? "null" : responseObject.getString("birthday");
                            String gender = responseObject.isNull("gender") ? "null" : responseObject.getString("gender");
                            String blood = responseObject.isNull("blood") ? "null" : responseObject.getString("blood");
                            int height = responseObject.isNull("height") ? -1 : responseObject.getInt("height");
                            int weight = responseObject.isNull("weight") ? -1 : responseObject.getInt("weight");

                            /* 保存到本地 */
                            editor.putString("telephone", telephone);
                            editor.putString("username", username);
                            editor.putString("birthday", birthday);
                            editor.putString("gender", gender);
                            editor.putString("blood", blood);
                            editor.putInt("height", height);
                            editor.putInt("weight", weight);
                            editor.apply();

                            downloadImage(telephone);    //下载图片到本地

                            intent = new Intent(LoginActivity.this, MainActivity.class); //跳转主页面
                            startActivity(intent);  //跳转至主活动
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 1:     //数据库中不存在此号码
                            intent = new Intent(LoginActivity.this, RegisterActivity.class); //跳转至注册页面
                            intent.putExtra("RegisterType", Constant.REGISTER_PHONE); //手机注册类型
                            intent.putExtra("Telephone", telTextView.getText().toString().trim());  //传手机号码给注册界面
                            startActivity(intent);
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

    public void loginByQQ(String qqid) {      //qq登录
        JSONObject jsonObject = new JSONObject();
        String requestJson = "";
        try {
            jsonObject.put("loginmethod", Constant.QQ_LOGIN);   //登录类型
            jsonObject.put("qqid", qqid);   //qq的openid
            requestJson = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
        request = new Request.Builder().url(Constant.BASE_URL + "login").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonString = response.body().string();
                    JSONObject responseObject = new JSONObject(jsonString);
                    int result = responseObject.getInt("result");
                    Intent intent = null;
                    switch (result) {
                        case 0:      //数据库中存在此QQ
                            /* 获取用户数据 */
                            String telephone = responseObject.isNull("telephone") ? "null" : responseObject.getString("telephone");
                            String username = responseObject.isNull("username") ? "null" : responseObject.getString("username");
                            String birthday = responseObject.isNull("birthday") ? "null" : responseObject.getString("birthday");
                            String gender = responseObject.isNull("gender") ? "null" : responseObject.getString("gender");
                            String blood = responseObject.isNull("blood") ? "null" : responseObject.getString("blood");
                            int height = responseObject.isNull("height") ? -1 : responseObject.getInt("height");
                            int weight = responseObject.isNull("weight") ? -1 : responseObject.getInt("weight");

                            /* 保存数据到本地 */
                            editor.putString("telephone", telephone);
                            editor.putString("username", username);
                            editor.putString("birthday", birthday);
                            editor.putString("gender", gender);
                            editor.putString("blood", blood);
                            editor.putInt("height", height);
                            editor.putInt("weight", weight);
                            editor.apply();

                            downloadImage(telephone);    //下载图片到本地

                            intent = new Intent(LoginActivity.this, MainActivity.class); //跳转主页面
                            startActivity(intent);  //跳转至主活动
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        case 1:     //数据库中不存在此QQ
                            intent = new Intent(LoginActivity.this, RegisterActivity.class); //跳转至注册页面
                            intent.putExtra("RegisterType", Constant.REGISTER_QQ_PHONE); //QQ注册方式
                            intent.putExtra("QQid", mTencent.getOpenId());  //传openid给注册界面
                            startActivity(intent);
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

    public void initSharePreferences() {    //初始化
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {   //保存状态,在onPause方法之后执行在onStop方法之前执行
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTencent == null) {
            Log.i("mTencent", "NULL");
        }
    }

    public void downloadImage(String telephone) { //下载图片到本地
        File dir = new File(Constant.LOCAL_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            Log.i("create dir", "success");
        }
        if (dir.exists() && dir.canWrite()) {
            final File file = new File(dir.getAbsolutePath(), "/" + telephone + ".jpg");    //本地图片名称为电话号码
            Log.i("file", file.getAbsolutePath());
            okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
            String imageurl = Constant.IMAGE_URL + telephone + ".jpg";
            request = new Request.Builder().url(imageurl).get().build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {   //读取网络图片到本地文件
                    InputStream is = response.body().byteStream();
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        fileOutputStream.write(buf, 0, len);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    is.close();
                    Log.i("download", "success");
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer.onFinish();
        }
        Log.i("LoginActivity", "onPause");
    }
}
