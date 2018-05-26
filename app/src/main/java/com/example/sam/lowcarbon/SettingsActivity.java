package com.example.sam.lowcarbon;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.suke.widget.SwitchButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
//    private AMapLocationListener aMapLocationListener;
//    private AMapLocationClient aMapLocationClient;
//    private AMapLocationClientOption aMapLocationClientOption;

    private SwitchButton shareButton;
    private Button backButton;
    private Boolean shareFlag;
    private String telephone;
    private Double lat;
    private Double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initSharePreferences();
        shareButton = (SwitchButton) findViewById(R.id.share_position);
        backButton = (Button) findViewById(R.id.back_button);
        initInfo();
//        initLocation();
        MyApplication.getLocation(new MyApplication.MyLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        lat = aMapLocation.getLatitude(); //纬度
                        lon = aMapLocation.getLongitude();//经度
                    } else {
                        Log.e("Failed", aMapLocation.getErrorCode() + aMapLocation.getErrorInfo());  //无法定位的情况
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void initInfo() {
        shareFlag = sharedPreferences.getBoolean("shareposition", false);
        if (shareFlag) {
            shareButton.setChecked(true);
        } else {
            shareButton.setChecked(false);
        }
        shareButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                shareFlag = !shareFlag;
                editor.putBoolean("shareposition", shareFlag);
                editor.apply();
                Log.e("sharePosition", Boolean.toString(sharedPreferences.getBoolean("shareposition", false)));
            }
        });
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    @Override
    public void onBackPressed() {
        if (!shareFlag) {
            lat = 0.0;
            lon = 0.0;
        }
        updateLocation();
//        aMapLocationClient.stopLocation();
        finish();
    }

//    public void initLocation() {
//        aMapLocationListener = new AMapLocationListener() {
//            @Override
//            public void onLocationChanged(AMapLocation aMapLocation) {
//                if (aMapLocation != null) {
//                    if (aMapLocation.getErrorCode() == 0) {
//                        lat = aMapLocation.getLatitude(); //纬度
//                        lon = aMapLocation.getLongitude();//经度
//                    } else {
//                        Log.e("Failed", aMapLocation.getErrorCode() + aMapLocation.getErrorInfo());  //无法定位的情况
//                    }
//                }
//            }
//        };
//        aMapLocationClient = new AMapLocationClient(SettingsActivity.this);
//        aMapLocationClient.setLocationListener(aMapLocationListener);
//        aMapLocationClientOption = new AMapLocationClientOption();      //定位模式
//        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);  //高精度定位：GPS和WIFI联合定位
//        aMapLocationClientOption.setInterval(10 * 60 * 1000);   //1分钟获取一次数据
//        aMapLocationClient.setLocationOption(aMapLocationClientOption);
//        aMapLocationClient.startLocation();     //开始定位
//    }

    public void updateLocation() {
        telephone = sharedPreferences.getString("telephone", "null");
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
}
