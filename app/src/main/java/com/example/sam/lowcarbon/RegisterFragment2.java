package com.example.sam.lowcarbon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mob.MobSDK;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import de.hdodenhof.circleimageview.CircleImageView;


public class RegisterFragment2 extends Fragment {

    private Listener mListener;
    private CircleImageView userImageView;
    private TextInputEditText userNameView;
    private Button submitButton;
    private InputMethodManager imm;

    public RegisterFragment2() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_fragment2, container, false);
        userImageView = (CircleImageView) view.findViewById(R.id.userimage_view);
        userNameView = (TextInputEditText) view.findViewById(R.id.username_view);
        submitButton = (Button) view.findViewById(R.id.submit_button);

        userNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    userNameView.clearFocus();
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });

        userImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.chooseImage();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userNameView.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), "用户名不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    mListener.submitRegister(userNameView.getText().toString().trim());
                }
            }
        });
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null) {
            imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setUserImageView(Bitmap bitmap) {
        userImageView.setImageBitmap(bitmap);
    }

    public Bitmap getUserImageView() {
        Bitmap bitmap = ((BitmapDrawable) userImageView.getDrawable()).getBitmap();
        return bitmap;
    }

    public interface Listener {
        public void chooseImage();
        public void submitRegister(String name);
    }
}
