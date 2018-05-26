package com.example.sam.lowcarbon;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mob.MobSDK;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

public class RegisterFragment1 extends Fragment {

    private Listener mListener;
    private TextInputEditText telTextView = null;
    private TextInputEditText codeTextView = null;
    private Button nextButton = null;
    private Button clearButton = null;
    private Button messageButton = null;
    private EventHandler eventHandler = null;


    public RegisterFragment1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMessage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_fragment1, container, false);
        telTextView = (TextInputEditText) view.findViewById(R.id.telephone_text);
        codeTextView = (TextInputEditText) view.findViewById(R.id.code_text);
        nextButton = (Button) view.findViewById(R.id.next_button);
        clearButton = (Button) view.findViewById(R.id.clear_button);
        messageButton = (Button) view.findViewById(R.id.message_button);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                telTextView.setText("");
            }
        });

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {  //若手机号格式正确
                    SMSSDK.getVerificationCode("86", telTextView.getText().toString());
                    messageButton.setClickable(false);
                    messageButton.getBackground().setAlpha(0);
                    CountDownTimer countDownTimer = new CountDownTimer(60050, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            Log.i("TimeLeft", millisUntilFinished + "");
                            messageButton.setText(millisUntilFinished / 1000 - 1 + "");
                        }

                        @Override
                        public void onFinish() {
                            messageButton.setClickable(true);
                            messageButton.setText("");
                            messageButton.getBackground().setAlpha(255);
                            messageButton.setBackgroundResource(R.drawable.ic_message);
                        }
                    };
                    countDownTimer.start();
                } else {    //否则
                    Toast.makeText(getContext(), "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (telTextView.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "手机号不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    if (!Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {
                        Toast.makeText(getContext(), "请输入正确的手机号", Toast.LENGTH_SHORT).show();
                    } else {
                        mListener.checkPhoneBindQQ(telTextView.getText().toString().trim());
                    }

//                    if (Constant.isChinaPhoneLegal(telTextView.getText().toString().trim())) {
//                        if (Constant.isCodeCorrest(codeTextView.getText().toString().trim())) {
//                            SMSSDK.submitVerificationCode("86", telTextView.getText().toString().trim(),
//                                    codeTextView.getText().toString().trim());    //提交短信验证码
//                        } else {
//                            Toast.makeText(getContext(), "验证码格式错误", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        Toast.makeText(getContext(), "请输入正确的手机号", Toast.LENGTH_SHORT).show();
//                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null) {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        SMSSDK.unregisterAllEventHandler();
    }

    public void initMessage() {
        MobSDK.init(getContext(), Constant.MOB_APPKEY, Constant.MOB_APPSECRET);
        eventHandler = new EventHandler() {
            @Override
            public void afterEvent(int event, int result, Object data) {
                switch (event) {
                    case SMSSDK.EVENT_GET_VERIFICATION_CODE:
                        if (result == SMSSDK.RESULT_COMPLETE) { //获取验证码成功
                            Toast.makeText(getContext(), "验证码已发送", Toast.LENGTH_SHORT).show();
                        } else {    //获取验证码失败
                            Toast.makeText(getContext(), "验证码发送失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE:
                        if (result == SMSSDK.RESULT_COMPLETE) { //验证码正确
                            mListener.checkPhoneBindQQ(telTextView.getText().toString().trim());
                        } else {    //验证码错误
                            Toast.makeText(getContext(), "验证码错误", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        SMSSDK.registerEventHandler(eventHandler);
    }

    public interface Listener {
        public void checkPhoneBindQQ(String tel);
    }
}
