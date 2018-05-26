package com.example.sam.lowcarbon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;
import android.widget.ImageView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.bumptech.glide.Glide;
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
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class MainActivity extends AppCompatActivity {

    /* UI控件 */
    private TextView cityText;
    private TextView weatherText;
    private TextView tempuratureText;
    private TextView windText;
    private TextView wetText;
    private ImageSwitcher imageSwitcher;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Button navigationButton;
    private TextView titleView;
    private Button sportsButton;
    private Button friendcircleButton;
    private Button moreButton;
    private LinearLayout weatherLayout;
    private ImageView weatherImageView;
    private CircleImageView userImageView;
    private TextView userNameView;
    private TextView stepsView;
    private LinearLayout stepsLayout;

    /* 定位监听 天气监听 广播监听*/
//    private AMapLocationListener aMapLocationListener;
//    private AMapLocationClient aMapLocationClient;
//    private AMapLocationClientOption aMapLocationClientOption;
    private WeatherSearchQuery weatherSearchQuery;
    private WeatherSearch weatherSearch;
    private Intent mainIntent;
    private BroadcastReceiver stepsReceiver;
    private BroadcastReceiver friendReceiver;

    /* 数值参数 */
    private int SizeOfWeatherImage = 0;
    private String city;
    private String district;
    private boolean isShow = true;
    private float touchDownX;
    private float touchUpX;
    private int[] arrayBackgrounds = new int[10];
    private String[] weathers = new String[40];
    private int[] weatherImages = new int[40];
    private int pictureIndex = 0;
    private int totalSteps;

    private SocketService socketService;
    private ServiceConnection serviceConnection;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private WebSocket clientSocket = null;
    private Timer stepTimer;
    private String telephone;
    private long period = 10 * 60 * 1000;
    private double lat;
    private double lon;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSharePreferences();
        telephone = sharedPreferences.getString("telephone", null);

        initBackgrounds();  //初始化背景图片
        loadWeatherImage();    //初始化天气

        /* UI初始化 */
        titleView = (TextView) findViewById(R.id.title);

        stepsLayout = (LinearLayout) findViewById(R.id.steps_layout);
        stepsView = (TextView) findViewById(R.id.steps_view);

        weatherLayout = (LinearLayout) findViewById(R.id.weather_layout);
        cityText = (TextView) findViewById(R.id.city_view);
        weatherText = (TextView) findViewById(R.id.weather_view);
        tempuratureText = (TextView) findViewById(R.id.tempurature_view);
        windText = (TextView) findViewById(R.id.wind_view);
        wetText = (TextView) findViewById(R.id.wet_view);
        weatherImageView = (ImageView) findViewById(R.id.weather_imageview);

        /* 跳转至运动活动 */
        sportsButton = (Button) findViewById(R.id.button_sports);
        sportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SportsActivity.class);
                intent.putExtra("Telephone", telephone);
                startActivity(intent);
            }
        });

        friendcircleButton = (Button) findViewById(R.id.button_friendcircle);   //朋友圈按钮
        friendcircleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FriendActivity.class);
                intent.putExtra("Telephone", telephone);
                startActivity(intent);
            }
        });

        moreButton = (Button) findViewById(R.id.button_more);   //更多按钮
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BreathTest.class));
            }
        });

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationButton = (Button) findViewById(R.id.button_navigation);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);   //打开侧栏
            }
        });

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setItemIconTintList(null);   //导航栏图标颜色设为原色
        navigationView.setItemTextAppearance(R.style.MenuStyle);    //设置导航栏文字样式

        /* 导航栏项目选择事件 */
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.nav_profile:
                        startActivityForResult(new Intent(MainActivity.this, UserInfoActivity.class), Constant.USER_INFO);
                        break;
                    case R.id.nav_logout:
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setMessage("您确定要退出当前登陆吗?");
                        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editor.clear().apply();
                                editor.putBoolean("login", false);
                                editor.apply();
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Code", Constant.LOGOUT_CODE);
                                startActivity(intent);
                            }
                        });
                        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                        break;
                    case R.id.nav_setting:
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_recommend:
                        startActivity(new Intent(MainActivity.this, RecommendActivity.class));
                        break;
                    case R.id.nav_app:
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        break;

                    default:
                        break;

                }
                drawerLayout.closeDrawer(GravityCompat.START);

                return true;
            }
        });

        userImageView = navigationView.getHeaderView(0).findViewById(R.id.user_ImageView);  //获取用户头像控件
        userNameView = navigationView.getHeaderView(0).findViewById(R.id.user_name);    //获取用户昵称控件

        /* 图片切换控件 */
        imageSwitcher = (ImageSwitcher) findViewById(R.id.imageView);
        imageSwitcher.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageResource(arrayBackgrounds[pictureIndex]); //获取图片资源id
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));    //设置图片样式撑满父控件
                imageView.setScaleType(ImageView.ScaleType.FIT_XY); //缩放样式
                return imageView;
            }
        });

        imageSwitcher.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {   //手指按下
                    touchDownX = motionEvent.getX();
                    return true;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {  //手指抬起
                    touchUpX = motionEvent.getX();
                    if (touchUpX - touchDownX > 100) {     //右滑动
                        pictureIndex = pictureIndex == 0 ? arrayBackgrounds.length - 1 : pictureIndex - 1;
                        imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_in_left));
                        imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_out_right));
                        imageSwitcher.setImageResource(arrayBackgrounds[pictureIndex]);
                    } else if (touchDownX - touchUpX > 100) {   //左滑动
                        pictureIndex = pictureIndex == arrayBackgrounds.length - 1 ? 0 : pictureIndex + 1;
                        imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_right));
                        imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_left));
                        imageSwitcher.setImageResource(arrayBackgrounds[pictureIndex]);
                    } else {    //否则显示或隐藏控件
                        if (isShow) {
                            hideAllView();
                        } else {
                            showAllView();
                        }
                        isShow = !isShow;   //标志位置反
                    }
                    return true;
                }
                return false;
            }
        });

        //步数上传数据库
        stepTimer = new Timer();
        stepTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Integer steps = sharedPreferences.getInt("steps", 0);
                JSONObject object = new JSONObject();
                String requestJson = null;
                try {
                    object.put("telephone", telephone);
                    object.put("steps", steps);
                    requestJson = object.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
                RequestBody body = new FormBody.Builder().add("requestJson", requestJson).build();
                Request request = new Request.Builder().url(Constant.BASE_URL + "steps").post(body).build();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("StepTimer", "Update failed!");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.e("StepTimer", "Update success!");
                    }
                });
            }
        }, 1000, period);

        //计步广播
        stepsReceiver = new BroadcastReceiver() {   //广播用于接受Service
            @Override
            public void onReceive(Context context, Intent intent) {
                totalSteps = intent.getExtras().getInt("Steps");   //从服务中获取步数
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stepsView.setText(String.valueOf(totalSteps));
                    }
                });
            }

        };

        IntentFilter stepfilter = new IntentFilter();
        stepfilter.addAction("com.example.sam.lowcarbon.stepscount");
        MainActivity.this.registerReceiver(stepsReceiver, stepfilter);  //设置广播

        //好友广播
        friendReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String from = intent.getExtras().getString("from");
                final String to = intent.getExtras().getString("to");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setMessage("用户" + from + "想添加您为好友?")
                                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        JSONObject jsonObject = new JSONObject();
                                        String requestJson = null;
                                        try {
                                            jsonObject.put("method", Constant.SOCKET_FRIEND_AGREE);
                                            jsonObject.put("user1", from);
                                            jsonObject.put("user2", to);
                                            requestJson = jsonObject.toString();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
                                        requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
                                        request = new Request.Builder().url(Constant.BASE_URL + "addfriend").post(requestBody).build();
                                        Call call = okHttpClient.newCall(request);
                                        call.enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
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
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(MainActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                });
                        dialog.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                JSONObject jsonObject = new JSONObject();
                                String requestJson = null;
                                try {
                                    jsonObject.put("method", Constant.SOCKET_FRIEND_REFUSE);
                                    jsonObject.put("user1", from);
                                    jsonObject.put("user2", to);
                                    requestJson = jsonObject.toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
                                requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
                                request = new Request.Builder().url(Constant.BASE_URL + "addfriend").post(requestBody).build();
                                Call call = okHttpClient.newCall(request);
                                call.enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }
                });
            }
        };

        IntentFilter friendfilter = new IntentFilter();
        friendfilter.addAction("com.example.sam.lowcarbon.friend");
        MainActivity.this.registerReceiver(friendReceiver, friendfilter);

        startService(new Intent(this, StepService.class));  //开始记步服务
        initIntent();       //初始化Intent
        runOnUiThread(new Runnable() {
            @Override
            public void run() {    //申请定位和存储权限
                AndPermission.with(MainActivity.this)
                        .permission(Permission.ACCESS_COARSE_LOCATION,
                                Permission.WRITE_EXTERNAL_STORAGE,
                                Permission.READ_CONTACTS)
                        .onGranted(new Action() {   //同意
                            @Override
                            public void onAction(List<String> permissions) {
                                initLocation();
                            }
                        })
                        .rationale(new Rationale() {   //拒绝一次
                            @Override
                            public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage("应用程序尚未完成授权，是否继续？")
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
                                            }
                                        })
                                        .show();
                            }
                        })
                        .onDenied(new Action() {   //禁止后不再询问
                            @Override
                            public void onAction(List<String> permissions) {
                                if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions)) {
                                    final SettingService service = AndPermission.permissionSetting(MainActivity.this);
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage("没有相应权限应用程序将无法使用，是否去系统设置中授权？")
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
                                                }
                                            })
                                            .show();
                                } else {
                                    Toast.makeText(MainActivity.this, "您取消了授权", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).start();
                updateUserInformation();
            }
        });

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                socketService = ((SocketService.MyBinder) service).getService();
                JSONObject object = new JSONObject();
                String msg = null;
                try {
                    object.put("method", Constant.SOCKET_ONLINE);
                    object.put("user", telephone);
                    msg = object.toString();
                    socketService.sendSocket(msg);
                    Log.i("msg", msg);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(new Intent(MainActivity.this, SocketService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    /* 获取Intent */
    public void initIntent() {
        mainIntent = getIntent();
        editor.putBoolean("login", true);   //登录
        editor.apply();
    }

    /* 获取背景图片 */
    public void initBackgrounds() {
        TypedArray temp = getResources().obtainTypedArray(R.array.backgrounds);
        int len = temp.length();
        arrayBackgrounds = new int[len];
        for (int i = 0; i < len; i++)
            arrayBackgrounds[i] = temp.getResourceId(i, 0);
        temp.recycle();
    }

    /* 初始化天气 */
    public void initWeather() {
        Log.i("Weather", "Init");
        weatherSearch = new WeatherSearch(this);
        weatherSearchQuery = new WeatherSearchQuery(city, WeatherSearchQuery.WEATHER_TYPE_LIVE);    //设置城市和模式参数
        weatherSearch.setQuery(weatherSearchQuery);
        weatherSearch.setOnWeatherSearchListener(new WeatherSearch.OnWeatherSearchListener() {
            @Override
            public void onWeatherLiveSearched(LocalWeatherLiveResult localWeatherLiveResult, int code) {
                if (code == 1000) {     //获取成功
                    if (localWeatherLiveResult != null && localWeatherLiveResult.getLiveResult() != null) {
                        weatherLayout.setBackgroundResource(R.drawable.weather_layout_background);
                        cityText.setText(city + "  " + district);
                        weatherText.setText("天气     " + localWeatherLiveResult.getLiveResult().getWeather());   //设置天气
                        tempuratureText.setText("温度     " + localWeatherLiveResult.getLiveResult().getTemperature() + "°C");    //设置温度
                        windText.setText(localWeatherLiveResult.getLiveResult().getWindDirection() + "风     " + localWeatherLiveResult.getLiveResult().getWindPower() + "级");   //设置风向
                        wetText.setText("湿度     " + localWeatherLiveResult.getLiveResult().getHumidity() + "%");    //设置湿度
                        for (int i = 0; i < SizeOfWeatherImage; ++i) {  //根据返回的天气情况加载对应的图片
                            if (weathers[i].equals(localWeatherLiveResult.getLiveResult().getWeather())) {
                                weatherImageView.setImageResource(weatherImages[i]);
                                break;
                            }
                        }
                    } else {
                        Log.e("Weather", "No Reuslt");
                        Toast.makeText(MainActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("Weather", code + "");
                    Toast.makeText(MainActivity.this, "获取地理信息失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {

            }
        });
        weatherSearch.searchWeatherAsyn();  //异步加载
    }

    /* 初始化定位服务 */
    public void initLocation() {
        MyApplication.getLocation(new MyApplication.MyLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        if (aMapLocation.getCity() != null) {   //定位获取到城市名称
                            city = aMapLocation.getCity();  //城市
                            district = aMapLocation.getDistrict();  //街区
                            initWeather();
                            //上传位置信息
                            if (sharedPreferences.getBoolean("shareposition", false)) {
                                lat = aMapLocation.getLatitude(); //纬度
                                lon = aMapLocation.getLongitude();//经度
                            } else {
                                lat = 0.0;
                                lon = 0.0;
                            }
                            updateLocation();
                        }
                    } else {
                        Log.e("Failed", aMapLocation.getErrorCode() + aMapLocation.getErrorInfo());  //无法定位的情况
                    }
                }
            }
        });
//        aMapLocationListener = new AMapLocationListener() {
//            @Override
//            public void onLocationChanged(AMapLocation aMapLocation) {
//                if (aMapLocation != null) {
//                    if (aMapLocation.getErrorCode() == 0) {
//                        if (aMapLocation.getCity() != null) {   //定位获取到城市名称
//                            city = aMapLocation.getCity();  //城市
//                            district = aMapLocation.getDistrict();  //街区
//                            initWeather();
//                            //上传位置信息
//                            if (sharedPreferences.getBoolean("shareposition", false)) {
//                                lat = aMapLocation.getLatitude(); //纬度
//                                lon = aMapLocation.getLongitude();//经度
//                            } else {
//                                lat = 0.0;
//                                lon = 0.0;
//                            }
//                            updateLocation();
//                        }
//                    } else {
//                        Log.e("Failed", aMapLocation.getErrorCode() + aMapLocation.getErrorInfo());  //无法定位的情况
//                    }
//                }
//            }
//        };
//
//        aMapLocationClient = new AMapLocationClient(MainActivity.this);
//        aMapLocationClient.setLocationListener(aMapLocationListener);
//        aMapLocationClientOption = new AMapLocationClientOption();      //定位模式
//        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);  //高精度定位：GPS和WIFI联合定位
//        aMapLocationClientOption.setInterval(10 * 60 * 1000);   //10分钟获取一次数据
//        aMapLocationClient.setLocationOption(aMapLocationClientOption);
//        aMapLocationClient.startLocation();     //开始定位
    }

    /* 天气图片初始化 */
    public void loadWeatherImage() {
        weathers = getResources().getStringArray(R.array.weather);
        TypedArray temp = getResources().obtainTypedArray(R.array.weatherimage);
        int len = temp.length();
        SizeOfWeatherImage = len;
        for (int i = 0; i < len; i++)
            weatherImages[i] = temp.getResourceId(i, 0);
        temp.recycle();
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    public void updateLocation() {
        JSONObject object = new JSONObject();
        String requestJson = null;
        try {
            object.put("telephone", telephone);
            object.put("wei", lat);
            requestJson = object.toString();
            object.put("jing", lon);
            requestJson = object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build();
        RequestBody body = new FormBody.Builder().add("requestJson", requestJson).build();
        Request request = new Request.Builder().url(Constant.BASE_URL + "position").post(body).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("position", "failed!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("position", "success!");
            }
        });

    }

    public void updateUserInformation() { //获取用户信息
        String username = sharedPreferences.getString("username", "null");
        userNameView.setText(username);
        // 本地读取
        File dir = new File(Constant.LOCAL_PATH);
        Log.i("dir", dir.getAbsolutePath());
        File file = new File(dir.getAbsolutePath() + "/" + telephone + ".jpg");
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        String imageurl = Constant.IMAGE_URL + telephone + ".jpg";  //组成图片url
        GlideApp.with(MainActivity.this)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(new BitmapDrawable(bitmap))
                .into(userImageView);
    }

    /* 显示所有控件 */
    public void showAllView() {
        Animation showAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.show); //渐显动画
        weatherLayout.setVisibility(View.VISIBLE);
        weatherLayout.setAnimation(showAnimation);
        titleView.setVisibility(View.VISIBLE);
        titleView.setAnimation(showAnimation);
        navigationButton.setVisibility(View.VISIBLE);
        navigationButton.setAnimation(showAnimation);
        sportsButton.setVisibility(View.VISIBLE);
        sportsButton.setAnimation(showAnimation);
        moreButton.setVisibility(View.VISIBLE);
        moreButton.setAnimation(showAnimation);
        friendcircleButton.setVisibility(View.VISIBLE);
        friendcircleButton.setAnimation(showAnimation);
        stepsLayout.setVisibility(View.VISIBLE);
        stepsLayout.setAnimation(showAnimation);
    }

    /* 隐藏所有控件 */
    public void hideAllView() {
        Animation hideAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hide);
        weatherLayout.setVisibility(View.GONE);
        weatherLayout.setAnimation(hideAnimation);
        titleView.setVisibility(View.GONE);
        titleView.setAnimation(hideAnimation);
        navigationButton.setVisibility(View.GONE);
        navigationButton.setAnimation(hideAnimation);
        sportsButton.setVisibility(View.GONE);
        sportsButton.setAnimation(hideAnimation);
        moreButton.setVisibility(View.GONE);
        moreButton.setAnimation(hideAnimation);
        friendcircleButton.setVisibility(View.GONE);
        friendcircleButton.setAnimation(hideAnimation);
        stepsLayout.setVisibility(View.GONE);
        stepsLayout.setAnimation(hideAnimation);
    }

    /* 回退键重载 */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {   //若侧栏打开则关闭侧栏
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);  //否则退出程序回到桌面
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.USER_INFO:  //从个人信息回调
                if (resultCode == RESULT_OK) {
                    updateUserInformation();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.this.unregisterReceiver(stepsReceiver);
        unbindService(serviceConnection);
//        aMapLocationClient.stopLocation();
        if (!telephone.equals("null")) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("method", Constant.SOCKET_OFFLINE);
                jsonObject.put("user", telephone);
                String json = jsonObject.toString();
                socketService.sendSocket(json);
                Log.i("MainActivity", "offline");
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity", "onPause");
    }

//    public void test() {
//        OkHttpClient client = new OkHttpClient.Builder().build();
//        Request request = new Request.Builder().url("ws://192.168.229.238:80").build();
//        client.newWebSocket(request, new WebSocketListener() {
//            @Override
//            public void onOpen(WebSocket webSocket, Response response) {
//                clientSocket = webSocket;
//                super.onOpen(webSocket, response);
//            }
//
//            @Override
//            public void onMessage(WebSocket webSocket, String text) {
//                super.onMessage(webSocket, text);
//            }
//
//            @Override
//            public void onMessage(WebSocket webSocket, ByteString bytes) {
//                super.onMessage(webSocket, bytes);
//            }
//
//            @Override
//            public void onClosing(WebSocket webSocket, int code, String reason) {
//                super.onClosing(webSocket, code, reason);
//            }
//
//            @Override
//            public void onClosed(WebSocket webSocket, int code, String reason) {
//                super.onClosed(webSocket, code, reason);
//                clientSocket = null;
//            }
//
//            @Override
//            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
//                super.onFailure(webSocket, t, response);
//            }
//        });
//    }
}
