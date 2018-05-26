package com.example.sam.lowcarbon;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;

public class StepService extends Service implements SensorEventListener {

    private SensorManager sManager;
    private Sensor mSensorAccelerometer;    //加速度传感器
    private float[] oriValues = new float[3];//存放三轴加速度
    private final int valueNum = 4;//数组元素个数
    private float[] tempValue = new float[valueNum];//暂存用于梯度计算阀值的数组
    private int tempCount = 0;//暂存元素个数
    private boolean isDirectionUp = false;//是否处于上升状态
    private int continueUpCount = 0;//上升次数
    private int continueUpFormerCount = 0;//前一点的上升次数
    private boolean lastStatus = false;//总体处于上升
    private float peakOfWave = 0;//波峰
    private float valleryOfWave = 0;//波谷
    private long timeOfThisPeak = 0;//此次波峰时间
    private long timeOfLastPeak = 0;//上次波峰（谷）时间
    private long timeOfNow = 0;//现在的时间
    private long gravityNew = 0;//现在的总加速度
    private long gravityOld = 0;//过去的总加速度
    private final float initialValue = (float) 1.3;//动态阀值的判断条件
    private float ThreadValue = (float) 2.0;//阀值的初始值
    private int mCount = 0;//总步数
    private Thread thread;
    private double speed;   //当前速度
    private int sportStatus;
    private boolean isSport = false;    //是否开启运动界面
    private boolean isBegin = false;
    private long thisTime = 0;
    private long lastTime = 0;

    private float[] threadList = new float[1000];
    private float[] waveList = new float[1000];
    private float[] waveList1 = new float[1000];
    private int countOfThread = 0;
    private int countOfWaves = 0;
    private int countOfWaves1 = 0;
    private float maxOfPeak = 0;
    private float temppeak1;

    private File datafile;
    private FileOutputStream fos;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public void setisSport(boolean newStatus) {
        isSport = newStatus;
        if (!isSport) {
            Arrays.fill(waveList, 0);
            Arrays.fill(threadList, 0);
            countOfThread = 0;
            countOfWaves = 0;
        }
    }

    public void initSharePreferences() {
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
//测试
//        datafile = new File(new File(Constant.LOCAL_PATH).getAbsolutePath(), "/data.txt");
//        if(datafile.exists()) {
//            datafile.delete();
//        }
//
//        try {
//            fos = new FileOutputStream(datafile, false);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        initSharePreferences();
        String lastTime = sharedPreferences.getString("laststeprecordtime", "null");
        if(lastTime.equals("null")) { //若不存在上一次记录,则记录本次时间为第一次
            Date date = new Date();
            editor.putString("laststeprecordtime", sdf.format(date));
            editor.apply();
        }
        mCount = sharedPreferences.getInt("steps", 0);  //查看有没有步数,没有就取0
        tempCount = 0;

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);    //传感器管理器
        mSensorAccelerometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);   //加速度传感器
        sManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);   //注册传感器，设置刷新时间为60ms
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Intent intent = new Intent();
                        intent.putExtra("Steps", mCount);
                        intent.setAction("com.example.sam.lowcarbon.stepscount");
                        sendBroadcast(intent);
                        String lastTime = sharedPreferences.getString("laststeprecordtime", "null");
                        if (lastTime.equals("null")) {
                            editor.putString("laststeprecordtime", sdf.format(new Date()));
                            editor.apply();
                        } else {
                            String currentTime = sdf.format(new Date());
                            if (!lastTime.equals(currentTime)) {    //不是同一天,步数清0
                                editor.putString("laststeprecordtime", currentTime);    //保存最后一次更新日期
                                mCount = 0;
                                editor.putInt("steps", mCount);
                                editor.apply();
                            } else {    //否则保存到本地
                                editor.putInt("steps", mCount);
                                editor.apply();
                            }
                        }

                        Thread.sleep(1000);    //每5s发送一次广播更新步数
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!thread.isAlive()) {
            thread.start();
        }

        Log.i("service", "onstartcommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {  //接口的方法，在这里获取三轴加速度，计算出当前的总加速度
        for (int i = 0; i < 3; ++i) {
            oriValues[i] = event.values[i];
//            测试
//            try {
//                fos.write((String.valueOf(event.values[i]).getBytes()));
//                fos.write(",".getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            fos.write("\r\n".getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
        }
        gravityNew = (long) Math.sqrt(oriValues[0] * oriValues[0] + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
        DetectorNewStep(gravityNew);

        if (isSport && countOfThread >= 30 && countOfWaves >= 30) { //在运动且组数大于20则判断一次
            Judge();
            Intent intent = new Intent();
            intent.putExtra("sportStatus", sportStatus);
            intent.setAction("com.example.sam.lowcarbon.sportstatus");
            sendBroadcast(intent);
        }
    }

    public void DetectorNewStep(long values) {
        //核心算法，判断步数：1 满足波峰判定 2 满足时间限制 3 满足阀值 4 将波峰与波谷之差纳入阀值的计算中去
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (DetectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleryOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    countStep();
                }
                if (timeOfNow - timeOfLastPeak >= 200 && (peakOfWave - valleryOfWave >= initialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = Peak_Valley_Thread(peakOfWave - valleryOfWave);
                }

            }
        }
        gravityOld = values;
    }

    public void countStep() {
        thisTime = System.currentTimeMillis();

        if (thisTime - lastTime > 2000) {
            tempCount = 0;
        } else {
//            if (this.tempCount <= 10)
//                isBegin = true;
            tempCount++;
        }

        if (tempCount >= 10) {
            if (tempCount == 10) {
//                for (int i=0; i<countOfWaves1; i++) {
//                    if (maxOfPeak < waveList1[i]) {
//                        maxOfPeak = waveList1[i];
//                    }
//                }
//                    temppeak1 = maxOfPeak;
//
//                    Arrays.fill(waveList1, 0);
//                    countOfWaves1 = 0;
//                    isBegin = false;
//                    maxOfPeak = 0;
//
//                    if (temppeak1 < 16) {
//                        this.tempCount = 0;
//                        return;
//                    }
                mCount += 10;
            } else {
                mCount++;
            }
        }

        lastTime = thisTime;
        Intent intent = new Intent();
        intent.putExtra("Steps", mCount);
        intent.setAction("com.example.sam.lowcarbon.stepscount");
        sendBroadcast(intent);
    }

    public boolean DetectorPeak(float newValue, float oldValue) {
        //判断波峰方法 1 由增到减 2 连续上升次数两次以上 3 波峰值满足一定区间
        //判断波谷方法 1 由减到增
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }
        if (!isDirectionUp && lastStatus && (continueUpFormerCount >= 2 && (oldValue >= 10 && oldValue <= 50))) {
            //判断波峰的条件，当图像一直增长，突然开始增加时，上个点认为是波峰，参数的变化请注意
            peakOfWave = oldValue;

            if (isSport && countOfWaves <= 500) {   //在运动的时候才记录数据
                waveList[countOfWaves++] = peakOfWave;
            }
//            if (isBegin && countOfWaves <= 500) {
//                waveList1[countOfWaves1++] = peakOfWave;
//            }

            return true;
        } else if (!lastStatus & isDirectionUp) {  //波谷同上
            valleryOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    public float Peak_Valley_Thread(float value) {
        float tempThread = ThreadValue;

        if (isSport && countOfThread <= 500) {      //在运动的时候才记录数据
            threadList[countOfThread++] = value;
        }

        if (tempCount < valueNum) {
            tempValue[tempCount++] = value;  //将差值都存入数组中
        } else {
            tempThread = averageValue(tempValue, valueNum); //梯度化差值函数，但是仍然未知为什么要这么做
            for (int i = 1; i < valueNum; i++) {   //将原有元素前移，腾出位置接收新元素
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[valueNum - 1] = value;//更新操作
        }
        return tempThread;
    }

    public float averageValue(float value[], int n) {   //梯度化阀值 有利于过滤杂波
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8.0)
            ave = 4.3f;
        else if (ave >= 7.0 && ave < 8.0)
            ave = 3.3f;
        else if (ave >= 4.0 && ave < 7.0)
            ave = 2.3f;
        else if (ave >= 3.0 && ave < 4.0)
            ave = 2.0f;
        else
            ave = 1.7f;
        return ave;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    } //当传感器的进度发生改变时会回调,这里缺省

    /**
     * new
     **/
    public int JudgeStatus() {
        float maxPeak = 0;
        for (int i = 10; i < 20; ++i) {
            if (waveList[i] - maxPeak > 0.0001) {
                maxPeak = waveList[i];
            }
        }

        float maxThread = 0;
        float aveThread = 0;
        for (int i = 10; i < 30; ++i) {
            if (threadList[i] - maxThread > 0.0001) {
                maxThread = threadList[i];
            }
            aveThread += threadList[i];
        }
        aveThread = aveThread / 20;

        if (speed > 2.0001 && speed < 10.0001) {
            if (maxPeak > 25.0001 && maxThread > 25.0001 && aveThread > 15.0001) {
                return 1;
            } else {
                return 2;
            }
        }
        return 0;
    }

    /**
     * new
     **/
    public void Judge() {
        sportStatus = JudgeStatus();
        Arrays.fill(waveList, 0);
        Arrays.fill(threadList, 0);
        countOfThread = 0;
        countOfWaves = 0;
    }

    public void setSpeed(double newSpeed) {
        this.speed = newSpeed;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("StepSerivce", "onDestory");
        sManager.unregisterListener(this);
        String lastTime = sharedPreferences.getString("laststeprecordtime", "null");
        if (lastTime.equals("null")) {
            editor.putString("laststeprecordtime", sdf.format(new Date()));
            editor.apply();
        } else {
            String currentTime = sdf.format(new Date());
            if (!lastTime.equals(currentTime)) {    //不是同一天,步数清0
                editor.putString("laststeprecordtime", currentTime);    //保存最后一次更新日期
                editor.putInt("steps", 0);
                editor.apply();
                mCount = 0;
            } else {    //否则保存到本地
                editor.putInt("steps", mCount);
                editor.apply();
            }
        }
    }

    public class MyBinder extends Binder {
        public StepService getService() {
            return StepService.this;
        }
    }
}
