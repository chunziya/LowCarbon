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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolylineOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dinuscxj.progressbar.CircleProgressBar;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
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
import java.text.DecimalFormat;
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

public class SportsActivity extends AppCompatActivity {

    /* 运动信息 */
    private boolean isFirstLocation = true;
    private double totalDistance = 0;
    private int progress = 0;
    private boolean isPressed = false;
    private boolean isSports = false;
    private double currentSpeed = 0;
    private double accuracy = 0;

    /* UI控件 */
    private MapView mMapView;
    private TextView countView;
    private AMap aMap;
    private Runnable runnable;
    private CircleProgressBar sportsButton;
    private TextView textView1;
    private TextView textView2;
    private Chronometer timeView;
    private ImageView greenBackground;
    private TextView accuracyView;
    private ImageView gpsStatusView;
    private ImageView sportStatusView;
    private FloatingActionsMenu mapStyle;
    private FloatingActionButton style1;
    private FloatingActionButton style2;
    private FloatingActionButton style3;
    private FloatingActionButton showButon;
    private Boolean showFriend = false;

    /* 定位参数 */
    private LatLng newLocation;
    private LatLng oldLocation;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;
    private AMapLocationListener aMapLocationListener;
    private PolylineOptions polylineOptions;
    private Double mLat;
    private Double mLon;

    /* 动画参数 */
    private ScaleAnimation scaleAnimation;
    private AlphaAnimation alphaAnimation;
    private AnimationSet animationSet;
    private DecimalFormat decimalFormat = new DecimalFormat("0.0"); //格式化参数

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private Handler handler;
    private StepService stepService;
    private ServiceConnection serviceConnection;
    private BroadcastReceiver statusReceiver;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private String telephone;
    private ArrayList<User> friendList;
    private Boolean firstShow;
    private ArrayList<Marker> markers;
    private BitmapDescriptor bitmapDescriptor;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sports);
        initSharePreferences();
        telephone = getIntent().getExtras().getString("Telephone").trim();

        accuracyView = (TextView) findViewById(R.id.strength_view); //当前定位精度
        /* 设置地图UI参数 */
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();
        aMap.setMyLocationEnabled(true);
        aMap.setMyLocationStyle(new MyLocationStyle().myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE));
        aMap.getUiSettings().setMyLocationButtonEnabled(true);   //我的位置
        aMap.getUiSettings().setZoomControlsEnabled(true);   //缩放控件
        aMap.getUiSettings().setScaleControlsEnabled(true);   //比例尺
        aMap.getUiSettings().setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
        aMap.animateCamera(CameraUpdateFactory.zoomTo(18));
        aMap.getUiSettings().setZoomGesturesEnabled(true);
        aMap.getUiSettings().setScrollGesturesEnabled(true);
        aMap.getUiSettings().setRotateGesturesEnabled(false);
        aMap.getUiSettings().setTiltGesturesEnabled(false);

        /* 设置运动信息UI参数 */
        countView = (TextView) findViewById(R.id.count_textview);
        textView1 = (TextView) findViewById(R.id.textview_1);
        textView2 = (TextView) findViewById(R.id.textview_2);
        timeView = (Chronometer) findViewById(R.id.time_view);
        greenBackground = (ImageView) findViewById(R.id.green_background);
        gpsStatusView = (ImageView) findViewById(R.id.gps_status_view);
        sportStatusView = (ImageView) findViewById(R.id.sportstatus_view);

        /* 开始运动按钮 */
        sportsButton = (CircleProgressBar) findViewById(R.id.start_button);
        sportsButton.setMax(200);   //进度条最大值
        sportsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ScaleAnimation temp = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);   //按下的时候按钮放大
                    temp.setDuration(800);
                    sportsButton.startAnimation(temp);
                    isPressed = true;
                    handler.post(runnable);     //通知线程更新进度
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    ScaleAnimation temp = new ScaleAnimation(1.1f, 1.0f, 1.1f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);   //手指抬起后按钮缩小
                    temp.setDuration(800);
                    sportsButton.startAnimation(temp);
                    isPressed = false;
                    progress = 0;   //进度置0
                    sportsButton.setProgress(0);
                    return false;
                }
                return false;
            }
        });

        /* 地图样式 */
        mapStyle = (FloatingActionsMenu) findViewById(R.id.map_style);
        style1 = (FloatingActionButton) findViewById(R.id.map_style1);
        style2 = (FloatingActionButton) findViewById(R.id.map_style2);
        style3 = (FloatingActionButton) findViewById(R.id.map_style3);
        style1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                mapStyle.collapse();
            }
        });
        style2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                mapStyle.collapse();
            }
        });
        style3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                mapStyle.collapse();
            }
        });

        /* 好友位置 */
        firstShow = true;
        showButon = (FloatingActionButton) findViewById(R.id.show_friend_position);
        showButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharedPreferences.getBoolean("shareposition", false)) {
                    showFriend = !showFriend;
                    if (showFriend) {
                        showButon.setIcon(R.drawable.ic_show);
                        showFriend();
                    } else {
                        showButon.setIcon(R.drawable.ic_unshow);
                        for (int i = 0; i < markers.size(); i++) {
                            markers.get(i).setVisible(false);
                        }
                    }
                } else {
                    new AlertDialog.Builder(SportsActivity.this)
                            .setMessage("开启位置共享服务才可以查看好友位置，是否前往设置？")
                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(SportsActivity.this, SettingsActivity.class));
                                }
                            })
                            .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            }
        });

        /* 申请权限 */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(0);
                    AndPermission.with(SportsActivity.this)
                            .permission(Permission.ACCESS_COARSE_LOCATION)
                            .onGranted(new Action() {   //同意
                                @Override
                                public void onAction(List<String> permissions) {
                                    initLocation();
                                }
                            })
                            .rationale(new Rationale() {   //拒绝一次
                                @Override
                                public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                                    new AlertDialog.Builder(SportsActivity.this)
                                            .setMessage("位置权限仍未授取，是否继续授权？")
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
                                    if (AndPermission.hasAlwaysDeniedPermission(SportsActivity.this, permissions)) {
                                        final SettingService service = AndPermission.permissionSetting(SportsActivity.this);
                                        new AlertDialog.Builder(SportsActivity.this)
                                                .setMessage("没有存储权限应用程序将无法运行，是否去系统设置中授权？")
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
                                        Toast.makeText(SportsActivity.this, "您取消了授权", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            }).start();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isSports) {   //若果没有开始运动
                    if (isPressed) {
                        progress = progress + 5;    //进度条+5
                        if (progress >= 200) {  //若进度条已满
                            isPressed = false;
                            progress = 0;   //进度条清0
                            sportsButton.setProgress(0);
                            countView.setVisibility(View.VISIBLE);  //运动信息界面显示
                            greenBackground.setVisibility(View.VISIBLE);
                            countTimer();   //进入运动倒计时
                        } else {    //否则每15毫秒更新一次进度条
                            sportsButton.setProgress(progress);
                            handler.postDelayed(runnable, 15);
                        }
                    }
                } else {    //已经开始运动
                    if (isPressed) {
                        progress = progress + 5;
                        if (progress >= 200) {  //同上
                            isPressed = false;
                            progress = 0;
                            sportsButton.setProgress(0);
                            sportsButton.setBackgroundResource(R.drawable.ic_start);
                            timeView.stop();    //计时器关闭
                            isSports = false;  //开始运动标志位重置
                            isFirstLocation = false;    //初始定位标志位重置
                            stepService.setisSport(false);
                        } else {
                            sportsButton.setProgress(progress);
                            handler.postDelayed(runnable, 30);  //否则30毫秒更新一次进度条
                        }
                    }
                }
            }
        };

        statusReceiver = new BroadcastReceiver() {   //广播用于接受Service
            @Override
            public void onReceive(Context context, Intent intent) {
                final int sportStatus = intent.getExtras().getInt("sportStatus");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (sportStatus) {
                            case 0:
                                sportStatusView.setImageResource(R.drawable.ic_walk_status);
                                break;
                            case 1:
                                sportStatusView.setImageResource(R.drawable.ic_run_status);
                                break;
                            case 2:
                                sportStatusView.setImageResource(R.drawable.ic_bike_status);
                                break;
                            default:
                                break;
                        }
                    }
                });
            }

        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.sam.lowcarbon.sportstatus");
        SportsActivity.this.registerReceiver(statusReceiver, filter);  //设置广播

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                stepService = ((StepService.MyBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(new Intent(SportsActivity.this, StepService.class), serviceConnection, BIND_AUTO_CREATE);

    }

    public void showFriend() {
        if (firstShow) {
            firstShow = false;
            okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("telephone", telephone);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String requestJson = jsonObject.toString();
            Log.e("friendList", requestJson);
            requestBody = new FormBody.Builder().add("requestJson", requestJson).build();
            request = new Request.Builder().url(Constant.BASE_URL + "friendlist").post(requestBody).build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(SportsActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonString = response.body().string();
                    try {
                        markers = new ArrayList<Marker>();
                        friendList = new ArrayList<User>();
                        JSONArray jsonArray = new JSONArray(jsonString);
                        for (int i = 0; i < jsonArray.length(); ++i) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            final String username = jsonObject.getString("username");
                            final String telephone = jsonObject.getString("telephone");
                            Double lat = jsonObject.getDouble("wei");
                            Double lon = jsonObject.getDouble("jing");
                            float distance = AMapUtils.calculateLineDistance(new LatLng(mLat, mLon), new LatLng(lat, lon));
                            Log.e("distance", Float.toString(distance));
                            if ((lat != 0 || lon != 0) && distance >= 0.0 && distance <= 5000.0) {
                                final LatLng latLng = new LatLng(lat, lon);
                                User user = new User(username, telephone);
                                friendList.add(user);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        customizeMarkerIcon(telephone, new OnMarkerIconLoadListener() {
                                            @Override
                                            public void markerIconLoadingFinished(View view) {
                                                markers.add(aMap.addMarker(new MarkerOptions()
                                                        .setFlat(true)
                                                        .position(latLng)
                                                        .title(username)
                                                        .icon(bitmapDescriptor)));
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            for (int i = 0; i < markers.size(); i++) {
                markers.get(i).setVisible(true);
            }
        }
        //Marker点击事件监听
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                aMap.setInfoWindowAdapter(new AMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        View infoWindow = LayoutInflater.from(SportsActivity.this).inflate(R.layout.marker_info, null);
                        TextView name = infoWindow.findViewById(R.id.maker_name);
                        TextView addr = infoWindow.findViewById(R.id.marker_addr);
                        name.setText(friendList.get(markers.indexOf(marker)).getUsername());
                        return infoWindow;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        return null;
                    }
                });
                marker.showInfoWindow();
                return false;
            }
        });
    }

    //加载好友图片到图标
    private void customizeMarkerIcon(String tele, final OnMarkerIconLoadListener listener) {
        final View markerView = LayoutInflater.from(this).inflate(R.layout.marker_bg, null);
        final CircleImageView icon = (CircleImageView) markerView.findViewById(R.id.marker_icon);
        String imageurl = Constant.IMAGE_URL + tele + ".jpg";
        GlideApp.with(this)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        icon.setImageDrawable(resource);
                        bitmapDescriptor = BitmapDescriptorFactory.fromView(markerView);
                        listener.markerIconLoadingFinished(markerView);
                    }
                });

    }

    public interface OnMarkerIconLoadListener {
        void markerIconLoadingFinished(View view);
    }

    public void drawGpsStatus() {   //信号强度
        if (accuracy > 40) {
            gpsStatusView.setImageResource(R.drawable.ic_gps_status_1);
        } else if (accuracy > 30) {
            gpsStatusView.setImageResource(R.drawable.ic_gps_status_2);
        } else if (accuracy > 20) {
            gpsStatusView.setImageResource(R.drawable.ic_gps_status_3);
        } else {
            gpsStatusView.setImageResource(R.drawable.ic_gps_status_4);
        }
    }

    /* 初始化定位 定位监听 */
    public void initLocation() {

        polylineOptions = new PolylineOptions();
        polylineOptions.setUseTexture(true);    //设置画线可以使用自定义png图片
        polylineOptions.setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.polyline));    //设置线条纹理
        polylineOptions.width(60);  //设置线条宽度

        /* 定位变化事件 */
        aMapLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) { //定位成功
                        accuracy = aMapLocation.getAccuracy();
                        mLat = aMapLocation.getLatitude(); //纬度
                        mLon = aMapLocation.getLongitude();//经度
                        drawGpsStatus();
                        if (isSports) {
                            if (accuracy <= 35) {   //若定位精度小于等于35则计算
                                newLocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());  //获取经纬度信息
                                currentSpeed = aMapLocation.getSpeed(); //获取当前速度
                                stepService.setSpeed(currentSpeed);
                                if (isFirstLocation) {
                                    isFirstLocation = false;
                                    oldLocation = newLocation;
                                } else {
                                    double distance = AMapUtils.calculateLineDistance(oldLocation, newLocation);    //计算每次的位移
                                    if (distance <= 2 * currentSpeed + 5 && distance >= 2 * currentSpeed - 5) {   //若每次的位移和当前速度下的位移差距太大则舍弃
                                        totalDistance += distance;  //否则将距离加入总距离
                                        aMap.addPolyline(polylineOptions);  //画线
                                        polylineOptions.add(newLocation);
                                        oldLocation = newLocation;  //坐标更新
                                    }
                                }
                                textView1.setText(decimalFormat.format(totalDistance)); //设置总距离
                                textView2.setText(decimalFormat.format(currentSpeed));  //设置当前速度
                                accuracyView.setText(decimalFormat.format(accuracy));   //设置当前精度（开发时使用）
                            } else {
                                //否则说明精度不好，提醒用户到开阔地
                            }
                        }
                    } else {
                        Log.e("Failed", aMapLocation.getErrorCode() + aMapLocation.getErrorInfo());  //无法定位的情况
                    }
                }
            }
        };

        /* 定位服务 定位设置 定位点属性设置 */
        aMapLocationClient = new AMapLocationClient(SportsActivity.this);
        aMapLocationClient.setLocationListener(aMapLocationListener);
        aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); //高精度模式
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.startLocation();
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    public void countTimer() {  //计时
        countView.setVisibility(View.VISIBLE);
        scaleAnimation = new ScaleAnimation(1.0f, 2.0f, 1.0f, 2.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);    //倒计时数字缩放
        scaleAnimation.setDuration(1000);
        alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(1000);
        animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setFillEnabled(true);
        animationSet.setFillAfter(true);
        CountDownTimer countDownTimer = new CountDownTimer(3900, 1000) {    //倒计时控件
            @Override
            public void onTick(long millisUntilFinished) {
                countView.setText(millisUntilFinished / 1000 + "");
                countView.startAnimation(animationSet);
            }

            @Override
            public void onFinish() {
                /* 倒计时结束后该UI控件不可见 */
                if (showFriend) {
                    showFriend = !showFriend;
                    showButon.setIcon(R.drawable.ic_unshow);
                    for (int i = 0; i < markers.size(); i++) {
                        markers.get(i).setVisible(false);
                    }
                }
                countView.setVisibility(View.GONE);
                greenBackground.setVisibility(View.GONE);
                sportsButton.setBackgroundResource(R.drawable.ic_pause);    //开始按钮变成暂停按钮
                isSports = true;
                isFirstLocation = true;
                stepService.setisSport(true);

                timeView.setBase(SystemClock.elapsedRealtime());    //运动计时器清零
                timeView.start();   //运动计时器开始
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (aMapLocationClient != null) {
            aMapLocationClient.onDestroy();
        }
        unregisterReceiver(statusReceiver);
        unbindService(serviceConnection);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {   //保存状态
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

}
