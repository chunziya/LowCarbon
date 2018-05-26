package com.example.sam.lowcarbon;

import android.app.Application;
import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

/**
 * Created by czc on 2018/1/17.
 */

public class MyApplication extends Application {
    // 声明AMapLocationClient类对象
    public static AMapLocationClient mLocationClient = null;
    // 声明AMapLocationClientOption对象
    public static AMapLocationClientOption mLocationOption = null;
    public static AMapLocation mLoction = null;

    private static Context mContext;
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }

    /**
     *
     * @ClassName: MyLocationListener
     * @Description: 定位回调
     * @author frank.fun@qq.com
     * @date 2016年11月10日 下午2:05:45
     *
     */
    public interface MyLocationListener {
        public void onLocationChanged(AMapLocation aMapLocation);
    }

    /**
     *
     * @Title: getLocation
     * @Description: 获取当前地址,不重新获取
     * @param listener
     */
    public static void getLocation(final MyLocationListener listener) {
        if (mLoction != null) {
            listener.onLocationChanged(mLoction);
            return;
        }
        getCurrentLocation(listener);
    }

    /**
     *
     * @Title: getCurrentLocation
     * @Description: 重新获取当前地址
     * @param listener
     */
    public static void getCurrentLocation(final MyLocationListener listener) {
        // 初始化定位
        mLocationClient = new AMapLocationClient(mContext);
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    mLocationClient.stopLocation();
                    mLoction = aMapLocation;
                    listener.onLocationChanged(aMapLocation);
                }
            }
        });
        // 初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);  //高精度定位：GPS和WIFI联合定位
        mLocationOption.setOnceLocationLatest(true); // 获取最近3s内精度最高的一次定位结果：
//        mLocationOption.setInterval(60 * 1000);  // 1分钟定位一次
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.startLocation();
    }
}
