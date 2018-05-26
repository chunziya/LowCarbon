package com.example.sam.lowcarbon;

import android.os.Bundle;
import android.os.SystemClock;
import android.os.TestLooperManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.util.Date;

/**
 * Created by abc on 2018/3/30.
 */

public class BreathTest extends AppCompatActivity {
    private CircleProgressBar breathButton;
    private Button exitButton;
    private Chronometer breathTime;
    private TextView text;
    private TextView times;
    private int breathCount = 0;
    private int actionDownCount = 0;
    private float currentX = 1.0f;
    private float currentY = 1.0f;
    private long interval;
    private Date a, b;
    private ScaleAnimation downAnimation = new ScaleAnimation(1.0f, 1.5f, 1.0f, 1.5f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    private ScaleAnimation upAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.breath);

        exitButton = (Button) findViewById(R.id.exit);
        breathTime = (Chronometer) findViewById(R.id.breath_time);
        text = (TextView) findViewById(R.id.breath_text);
        times = (TextView) findViewById(R.id.times);
        breathButton = (CircleProgressBar) findViewById(R.id.breath_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BreathTest.super.onBackPressed();
            }
        });
        breathButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    a = new Date();
                    actionDownCount++;
                    breathTime.setBase(SystemClock.elapsedRealtime());
                } else if (event.getAction() == MotionEvent.ACTION_UP && actionDownCount == breathCount) {
                    b = new Date();
                    interval = b.getTime() - a.getTime();
                    if (interval > 1 && interval <= 2000) {
                        currentX = 1.0f + 0.5f / 2000 * interval;
                        currentY = 1.0f + 0.5f / 2000 * interval;
                        Log.e("zuobiao", Float.toString(currentX) + "," + Float.toString(currentY));
                        upAnimation = new ScaleAnimation(currentX, 1.0f, currentY, 1.0f,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    } else {
                        upAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    }
                    AnimationSet animationSet = new AnimationSet(true);
                    animationSet.setDuration(1500);     //缩小时间
                    animationSet.addAnimation(upAnimation);
                    breathButton.startAnimation(animationSet);
                    animationSet.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            text.setText("呼气");
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            breathTime.stop();
                            text.setText("按下开始练习");
                            times.setText("本次练习次数：" + Integer.toString(breathCount));
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
                return false;
            }
        });

        breathButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                breathCount++;
                a = new Date();
                breathTime.start();
                actionDownCount = breathCount;
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.setDuration(2000);     //放大时间
                animationSet.addAnimation(downAnimation);
                animationSet.setFillEnabled(true);
                animationSet.setFillAfter(true);
                breathButton.startAnimation(animationSet);
                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        text.setText("吸气");
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        text.setText("摒住");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                return true;
            }
        });
    }


}
