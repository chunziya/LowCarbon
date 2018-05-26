package com.example.sam.lowcarbon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.OptionsPickerView;
import com.bigkoo.pickerview.TimePickerView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.SettingService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserInfoActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private OkHttpClient okHttpClient;
    private RequestBody requestBody;
    private Request request;
    private File file;

    private OptionsPickerView genderOptions, bloodOptions, heightOptions, weightOptions;
    private TimePickerView birthTime;
    private ArrayList<String> genderItem = new ArrayList<>();
    private ArrayList<String> bloodItem = new ArrayList<>();
    private ArrayList<String> heightItem = new ArrayList<>();
    private ArrayList<String> weightItem = new ArrayList<>();

    private CircleImageView userImageView;
    private TextInputEditText userNameView;
    private InputMethodManager imm;
    private TextView genderView;
    private TextView genderClick;
    private TextView birthdayView;
    private TextView birthdayClick;
    private TextView bloodView;
    private TextView bloodClick;
    private TextView heightView;
    private TextView heightClick;
    private TextView weightView;
    private TextView weightClick;
    private Button saveButton;
    private Button backButton;

    private String username;
    private String gender;
    private String birthday;
    private String blood;
    private String telephone;
    private int height;
    private int weight;
    private int flagChanged = 0b000000;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat sdf;
    private int selectedGender = 0;
    private int selectedBlood = 0;
    private int selectedHeight = 50;
    private int selectedWeight = 30;

    @SuppressLint({"ClickableViewAccessibility", "SimpleDateFormat"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        initSharePreferences();

        /*  UI初始化  */
        userImageView = (CircleImageView) findViewById(R.id.user_ImageView);
        userNameView = (TextInputEditText) findViewById(R.id.name_view);
        genderView = (TextView) findViewById(R.id.gender_view);
        genderClick = (TextView) findViewById(R.id.gender);
        birthdayView = (TextView) findViewById(R.id.birthday_view);
        birthdayClick = (TextView) findViewById(R.id.birthday);
        bloodView = (TextView) findViewById(R.id.blood_view);
        bloodClick = (TextView) findViewById(R.id.blood);
        heightView = (TextView) findViewById(R.id.height_view);
        heightClick = (TextView) findViewById(R.id.height);
        weightView = (TextView) findViewById(R.id.weight_view);
        weightClick = (TextView) findViewById(R.id.weight);
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        userNameView.setCursorVisible(false);
        userNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userNameView.setCursorVisible(true);
                flagChanged = flagChanged | 1 << 5;
//                userNameView.setSelection(userNameView.getText().length());
            }
        });
        userNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    userNameView.setCursorVisible(false);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return true;
            }
        });

        saveButton = (Button) findViewById(R.id.save_button);
        backButton = (Button) findViewById(R.id.back_button);
        userImageView.setOnClickListener(this);
        genderClick.setOnClickListener(this);
        birthdayClick.setOnClickListener(this);
        bloodClick.setOnClickListener(this);
        heightClick.setOnClickListener(this);
        weightClick.setOnClickListener(this);
        backButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(0);
                    AndPermission.with(UserInfoActivity.this)
                            .permission(Permission.WRITE_EXTERNAL_STORAGE)
                            .onGranted(new Action() {   //同意
                                @Override
                                public void onAction(List<String> permissions) {}
                            })
                            .rationale(new Rationale() {   //拒绝一次
                                @Override
                                public void showRationale(Context context, List<String> permissions, final RequestExecutor executor) {
                                    new AlertDialog.Builder(UserInfoActivity.this)
                                            .setMessage("存储权限仍未授取，是否继续授权？")
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
                                    if (AndPermission.hasAlwaysDeniedPermission(UserInfoActivity.this, permissions)) {
                                        final SettingService service = AndPermission.permissionSetting(UserInfoActivity.this);
                                        new AlertDialog.Builder(UserInfoActivity.this)
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
                                        Toast.makeText(UserInfoActivity.this, "您取消了授权", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            }).start();
                    initUserInformation();  //初始化用户信息

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.user_ImageView:
                chooseImage();
                break;
            case R.id.gender:
                initGenderPicker();
                break;
            case R.id.birthday:
                initBirthdayPicker();
                break;
            case R.id.blood:
                initBloodPicker();
                break;
            case R.id.height:
                initHeightPicker();
                break;
            case R.id.weight:
                initWeightPicker();
                break;
            case R.id.save_button:
                updateUserInformation();
                break;
            case R.id.back_button:
                onBackPressed();
                break;
            default:
        }
    }

    public void initOptionsData() {
        genderItem.add("男");
        genderItem.add("女");
        bloodItem.add("O");
        bloodItem.add("A");
        bloodItem.add("B");
        bloodItem.add("AB");
        for (int i = 0; i <= 100; ++i) {
            heightItem.add(Integer.toString(i + 120));
        }
        for (int i = 0; i <= 120; ++i) {
            weightItem.add(Integer.toString(i + 30));
        }
    }

    public void initUserInformation() {   //本地获取用户信息
        username = sharedPreferences.getString("username", "");
        gender = sharedPreferences.getString("gender", "null");
        birthday = sharedPreferences.getString("birthday", "null");
        blood = sharedPreferences.getString("blood", "null");
        height = sharedPreferences.getInt("height", -1);
        weight = sharedPreferences.getInt("weight", -1);
        userNameView.setText(username);
        initOptionsData();

        if (birthday.equals("null")) {  //生日需要格式化
//            try {
            birthdayView.setText("请选择生日");
//                selectedDate.setTime(sdf.parse("2000-01-01"));
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
        } else {
            try {
                birthdayView.setText(birthday);
                selectedDate.setTime(sdf.parse(birthday));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (gender.equals("null")) {
            genderView.setText("请选择性别");
        } else {
            genderView.setText(gender);
            selectedGender = genderItem.indexOf(gender);
        }

        if (blood.equals("null")) {
            bloodView.setText("请选择血型");
        } else {
            bloodView.setText(blood);
            selectedBlood = bloodItem.indexOf(blood);
        }

        if (height == -1 || height == 0) {
            heightView.setText("请选择身高");
        } else {
            heightView.setText(Integer.toString(height) + "cm");
            selectedHeight = heightItem.indexOf(Integer.toString(height));
        }

        if (weight == -1 || weight == 0) {
            weightView.setText("请选择体重");
        } else {
            weightView.setText(Integer.toString(weight) + "kg");
            selectedWeight = weightItem.indexOf(Integer.toString(weight));
        }

        telephone = sharedPreferences.getString("telephone", "null");
        // 本地读取
        File dir = new File(Constant.LOCAL_PATH);
        Log.i("dir", dir.getAbsolutePath());
        File file = new File(dir.getAbsolutePath() + "/" + telephone + ".jpg");
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        String imageurl = Constant.IMAGE_URL + telephone + ".jpg";
        GlideApp.with(UserInfoActivity.this)
                .load(imageurl)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(new BitmapDrawable(bitmap))
                .into(userImageView);
//        File dir = new File(Constant.LOCAL_PATH);
//        Log.i("dir", dir.getAbsolutePath());
//        File file = new File(dir.getAbsolutePath() + "/" + telephone + ".jpg");
//        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//        userImageView.setImageBitmap(bitmap);
    }

    public void initBirthdayPicker() {
        birthTime = new TimePickerView.Builder(this, new TimePickerView.OnTimeSelectListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onTimeSelect(Date date, View v) {//选中事件回调
                // 这里回调过来的v,就是show()方法里面所添加的 View 参数，如果show的时候没有添加参数，v则为null
                birthdayView.setText(new SimpleDateFormat("yyyy-MM-dd").format(date)); //可根据需要自行截取数据显示
                selectedDate.setTime(date);
                flagChanged = flagChanged | 1 << 4;
                Log.e("BirthdayFlag", Integer.toBinaryString(flagChanged));
            }
        })
                .isCyclic(true)
                .setSubCalSize(15)//确定和取消文字大小
                .setTitleText("选择生日")//标题文字
                .setTitleSize(18)//标题文字大小
                .setDate(selectedDate)// 如果不设置的话，默认是系统时间
                .setType(new boolean[]{true, true, true, false, false, false})
                .setLabel("", "", "", "", "", "")
                .isCenterLabel(false)
                .build();
        birthTime.show();
    }

    public void initGenderPicker() {
        genderOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                String tx = genderItem.get(options1);
                selectedGender = options1;
                genderView.setText(tx);
                flagChanged = flagChanged | 1 << 3;
            }
        })
                .setSubCalSize(15)//确定和取消文字大小
                .setTitleText("选择性别")//标题
                .setTitleSize(18)//标题文字大小
                .setSelectOptions(selectedGender)
                .isDialog(true)
                .build();
        genderOptions.setPicker(genderItem);
        genderOptions.show();
    }

    public void initBloodPicker() {
        //条件选择器
        bloodOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) { //选中事件回调
                String tx = bloodItem.get(options1);
                selectedBlood = options1;
                bloodView.setText(tx);
                flagChanged = flagChanged | 1 << 2;
            }
        })
                .setSubCalSize(15)//确定和取消文字大小
                .setTitleText("选择血型")//标题
                .setTitleSize(18)//标题文字大小
                .setCyclic(true, false, false)
                .setSelectOptions(selectedBlood)
                .isDialog(true)
                .build();
        bloodOptions.setPicker(bloodItem);
        bloodOptions.show();
    }

    public void initHeightPicker() {
        //条件选择器
        heightOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) { //选中事件回调
                String tx = heightItem.get(options1);
                selectedHeight = options1;
                heightView.setText(tx + "cm");
                flagChanged = flagChanged | 1 << 1;
            }
        })
                .setSubCalSize(15)//确定和取消文字大小
                .setTitleText("选择身高")//标题
                .setTitleSize(18)//标题文字大小
                .setSelectOptions(selectedHeight)
                .setLabels("cm              ", "", "")
                .isCenterLabel(true) //是否只显示中间选中项的label文字，false则每项item全部都带有label
                .isDialog(true)
                .build();
        heightOptions.setPicker(heightItem);
        heightOptions.show();
    }

    public void initWeightPicker() {
        //条件选择器
        weightOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int option2, int options3, View v) { //选中事件回调
                String tx = weightItem.get(options1);
                selectedWeight = options1;
                weightView.setText(tx + "kg");
                flagChanged = flagChanged | 1;
            }
        })
                .setSubCalSize(15)//确定和取消文字大小
                .setTitleText("选择体重")//标题
                .setTitleSize(18)//标题文字大小
                .setSelectOptions(selectedWeight)
                .setLabels("kg              ", "", "")
                .isDialog(true)
                .build();
        weightOptions.setPicker(weightItem);
        weightOptions.show();
    }

    public void initSharePreferences() {    //初始化
        editor = getSharedPreferences("LowCarbon", MODE_PRIVATE).edit();    //不存在则创建sharepreferences对象
        sharedPreferences = getSharedPreferences("LowCarbon", MODE_PRIVATE);    //获取sharepreferences对象
    }

    public void updateUserInformation() {
        username = userNameView.getText().toString();
        gender = genderView.getText().toString();
        birthday = birthdayView.getText().toString();
        blood = bloodView.getText().toString();
        if (birthday.equals("请选择生日")) {
            birthday = "null";
        }
        if (gender.equals("请选择性别")) {
            gender = "null";
        }
        if (blood.equals("请选择血型")) {
            blood = "null";
        }
        if (heightView.getText().toString().equals("请选择身高")) {
            height = -1;
        } else {
            height = Integer.parseInt(heightView.getText().toString().replace("cm", ""));
        }
        if (weightView.getText().toString().equals("请选择体重")) {
            weight = -1;
        } else {
            weight = Integer.parseInt(weightView.getText().toString().replace("kg", ""));
        }

        if (username.trim().isEmpty()) {
            Toast.makeText(UserInfoActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
        } else {
            String telephone = sharedPreferences.getString("telephone", "null");
            JSONObject jsonObject = new JSONObject();
            String requestJson = "";
            try {
                jsonObject.put("flagChanged", flagChanged);
                jsonObject.put("telephone", telephone); //电话号码
                jsonObject.put("username", username);   //用户名
                jsonObject.put("birthday", birthday);    //生日
                jsonObject.put("gender", gender);        //性别
                jsonObject.put("blood", blood);          //血型
                jsonObject.put("height", height);        //身高
                jsonObject.put("weight", weight);        //体重
                requestJson = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            File image = getImageFile();
            okHttpClient = new OkHttpClient.Builder().readTimeout(3, TimeUnit.SECONDS).connectTimeout(3, TimeUnit.SECONDS).build();
            requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("requestJson", requestJson)
                    .addFormDataPart("userImage", "userImage.jpg", RequestBody.create(Constant.FILE, image))
                    .build();
            request = new Request.Builder().url(Constant.BASE_URL + "userinfo").post(requestBody).build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {


                    Log.e("UserInfoActivity", "jsonObject failed!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UserInfoActivity.this, "网络异常,请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    Log.e("UserInfoActivity", "jsonObject successed!");
                    try {
                        String jsonString = response.body().string();
                        Log.e("UserInfoActivity", jsonString);

                        JSONObject responseObject = new JSONObject(jsonString);
                        int result = responseObject.getInt("result");
                        if (result == 1) {  //保存成功
                        /* 数据保存在本地 */
                            editor.putString("username", username);
                            editor.putString("gender", gender);
                            editor.putString("birthday", birthday);
                            editor.putString("blood", blood);
                            editor.putInt("height", height);
                            editor.putInt("weight", weight);
                            editor.apply();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(UserInfoActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(UserInfoActivity.this, "保存失败,请稍后再试", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void setUserImageView(Bitmap bitmap) {
        userImageView.setImageBitmap(bitmap);
    }

    public Bitmap getUserImageView() {
        Bitmap bitmap = ((BitmapDrawable) userImageView.getDrawable()).getBitmap();
        return bitmap;
    }

    public File getImageFile() {    //用户选择完头像后将头像保存到本地
        Bitmap bitmap = getUserImageView();
        File dir = new File(Constant.LOCAL_PATH);
        if (dir.exists() && dir.canWrite()) {
            file = new File(dir.getAbsolutePath() + "/" + telephone + ".jpg");     //创建文件
            Log.i("dirpath", dir.getAbsolutePath());
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                FileOutputStream out = new FileOutputStream(file);  //将bitmap读取到本地文件下
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Log.i("fileoutstream", "success");
                out.flush();
                out.close();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i("dirs", "failed");
        }
        return null;
    }

    /* 选择并剪裁图片 */
    public void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, Constant.PHOTO_REQUEST_GALLERY);
    }

    public void crop(Uri uri) {
        // 裁剪图片意图
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // 裁剪框的比例，1：1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);

        intent.putExtra("outputFormat", "JPEG");// 图片格式
        intent.putExtra("noFaceDetection", true);// 取消人脸识别
        intent.putExtra("return-data", true);
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CUT
        startActivityForResult(intent, Constant.PHOTO_REQUEST_CUT);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //活动回调
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.PHOTO_REQUEST_GALLERY:    //从相册回调
                if (data != null) {
                    Log.i("Gallery", "Success");
                    Uri uri = data.getData();   //获取选择图片的uri
                    crop(uri);  //剪裁图片
                }
                break;
            case Constant.PHOTO_REQUEST_CUT:    //剪裁回调
                if (data != null) {
                    Bitmap bitmap = data.getParcelableExtra("data");
                    setUserImageView(bitmap);
                }
                break;
            default:
                break;
        }
    }

}
