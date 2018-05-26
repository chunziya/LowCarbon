package com.example.sam.lowcarbon;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.florent37.materialviewpager.header.MaterialViewPagerHeaderDecorator;
import com.melnykov.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.BIND_AUTO_CREATE;

public class FriendRecommendFragment extends Fragment {

    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;

    private FriendRecommendAdapter friendRecommendAdapter;
    private OnFragmentInteractionListener mListener;
    private Handler handler = new Handler();
    private RecyclerView recommendView;
    private FloatingActionButton searchButton;
    private TextView blankView;
    private List<User> friendPhone;
    private String telephone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_recommend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        telephone = getActivity().getIntent().getExtras().getString("Telephone").trim();
        blankView = view.findViewById(R.id.blank_1);
        searchButton = view.findViewById(R.id.fab);
        recommendView = view.findViewById(R.id.recommend_recyclerView);
        recommendView.setHasFixedSize(true);   //设置滚动惯性平滑等
        recommendView.setNestedScrollingEnabled(false);
        getContactsFriend();

        searchButton.attachToRecyclerView(recommendView);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void getContactsFriend() {
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    .replace(" ", "").replace("+86", "");
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("phoneNum", phone);
                jsonObject.put("phoneName", name);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("telephone", telephone);
            jsonObject.put("method", Constant.FRIEND_CONTACTS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        okHttpClient = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
        requestBody = new FormBody.Builder().add("requestJsonArray", jsonArray.toString()).add("requestJson", jsonObject.toString()).build();
        request = new Request.Builder().url(Constant.BASE_URL + "getcontacts").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        blankView.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseJson = response.body().string();
                Log.e("phoneFriend", "success!");
                try {
                    final JSONArray responseArray = new JSONArray(responseJson);
                    friendPhone = new ArrayList<User>();
                    for (int i = 0; i < responseArray.length(); i++) {
                        JSONObject jsonObject = responseArray.getJSONObject(i);
                        String name = jsonObject.getString("username") + " (" + jsonObject.getString("phoneName") + ")";
                        String telephone = jsonObject.getString("telephone");
                        User user = new User(name, telephone);
                        friendPhone.add(user);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (responseArray.length() == 0) {
//                                blankView.setVisibility(View.VISIBLE);
                            } else {
                                friendRecommendAdapter = new FriendRecommendAdapter(R.layout.friend_recommend_item, friendPhone);
                                recommendView.setLayoutManager(new LinearLayoutManager(getContext()));
                                recommendView.addItemDecoration(new MaterialViewPagerHeaderDecorator());
                                recommendView.setAdapter(friendRecommendAdapter);
                                friendRecommendAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                                    @Override
                                    public void onItemChildClick(BaseQuickAdapter adapter, final View view, final int position) {
                                        new MaterialDialog.Builder(getContext())
                                                .backgroundColorRes(R.color.white)
                                                .content("确定添加 " + friendPhone.get(position).getUsername() + " 为好友？")
                                                .contentColorRes(R.color.black_semi_transparent)
                                                .negativeText("取消")
                                                .positiveText("确定")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        mListener.addFriend(telephone, friendPhone.get(position).getTelephone());
                                                        Button button = view.findViewById(R.id.add_button);
                                                        button.setClickable(false);
                                                        button.setBackgroundColor(0);
                                                        button.setTextColor(Color.BLACK);
                                                        button.setText("已发送");
                                                    }
                                                })
                                                .show();
                                    }
                                });
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static FriendRecommendFragment newInstance() {
        return new FriendRecommendFragment();
    }

    public interface OnFragmentInteractionListener {
        public void addFriend(String from, String to);
    }

}
