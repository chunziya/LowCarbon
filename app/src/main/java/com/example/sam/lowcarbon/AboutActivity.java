package com.example.sam.lowcarbon;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

public class AboutActivity extends AppCompatActivity {

    private Button backButton;
    private LinearLayout view1;
    private LinearLayout view2;
    private LinearLayout view3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        backButton = (Button) findViewById(R.id.back_button);
        view1 = (LinearLayout) findViewById(R.id.content_1);
        view2 = (LinearLayout) findViewById(R.id.content_2);
        view3 = (LinearLayout) findViewById(R.id.content_3);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(AboutActivity.this)
                        .title("功能介绍")
                        .content("本APP实现了对个体用户运动数据的实时采集、存储和展示，和对全体用户数据的统计、分析和排名等功能。")
                        .backgroundColorRes(R.color.white)
                        .contentColorRes(R.color.calender_black)
                        .titleColorRes(R.color.primary_pressed)
                        .show();
            }
        });
        view2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(AboutActivity.this)
                        .title("联系方式")
                        .content("邮箱：1004143102@cugb.edu.cn\n电话：15230735830")
                        .backgroundColorRes(R.color.white)
                        .contentColorRes(R.color.calender_black)
                        .titleColorRes(R.color.primary_pressed)
                        .show();
            }
        });
        view3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(AboutActivity.this)
                        .title("特别声明")
                        .content("\"基于Android智能终端的运动数据采集及分析系统\"为中国地质大学信息工程学院电子信息工程专业1004143102本科毕业设计。\n" +
                                "\n特别鸣谢@指导老师 张玉清副教授")
                        .backgroundColorRes(R.color.white)
                        .contentColorRes(R.color.calender_black)
                        .titleColorRes(R.color.primary_pressed)
                        .show();
            }
        });
    }
}
