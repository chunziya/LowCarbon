package com.example.sam.lowcarbon;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sam on 2017/11/12.
 */

public class Constant {

    public static final int LOGOUT_CODE = -1;


    public static final String QQ_APPID = "1106496066";     //QQ登录用ID
    public static final MediaType FILE = MediaType.parse("application/octet-stream");

    //    public static final String SOCKET_IP = "121.194.93.124";
    public static final String SOCKET_IP = "192.168.229.206";
//    public static final String SOCKET_IP = "192.168.229.174";  //本地服务器
    public static final int SOCKET_PORT = 9000;
transient 
    public static final String BASE_URL = "http://" + SOCKET_IP + ":8080/LowCarbonService/";      //服务器路径

    public static final String IMAGE_URL = "http://" + SOCKET_IP + ":8080/UserImage/";    //服务器图片路径
    public static final String MOB_APPKEY = "223297094c9e2";
    public static final String MOB_APPSECRET = "bfe0b587d52cc92c9f70cd5cd674bccf";
    public static final String LOCAL_PATH = Environment.getExternalStorageDirectory() + "/LowCarbon";  //本地存储路径


    public static final int LOCATION_CODE = 1000;
    public static final int QQ_LOGIN = 1001;
    public static final int WECHAT_LOGIN = 1002;
    public static final int PHONE_LOGIN = 1003;
    public static final int STEPS_CODE = 1004;
    public static final int SD_CODE = 1005;

    public static final int REGISTER_PHONE = 2000;
    public static final int REGISTER_QQ_PHONE = 2001;
    public static final int REGISTER_WECHAT_PHONE = 2002;

    public static final int PHOTO_REQUEST_GALLERY = 100;// 从本地相册中选择
    public static final int PHOTO_REQUEST_CUT = 101;// 结果

    public static final int SOCKET_ONLINE = 3000;
    public static final int SOCKET_OFFLINE = 3001;
    public static final int SOCKET_FRIEND_ADD = 3002;
    public static final int SOCKET_FRIEND_DELETE = 3003;
    public static final int SOCKET_FRIEND_AGREE = 3004;
    public static final int SOCKET_FRIEND_REFUSE = 3005;
    public static final int SOCKET_RESPONSE = 3006;

    public static final int USER_INFO = 4000;
    public static final int FRIEND_CONTACTS = 4001;
    public static final int FRIEND_SEARCH = 4002;

    /* 正则表达式大陆手机号识别 */
    public static boolean isChinaPhoneLegal(String str) throws PatternSyntaxException {
        String regExp = "^((13[0-9])|(15[^4])|(18[0,2,3,5-9])|(17[0-8])|(147))\\d{8}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /* 判断验证码是否为4位数字 */
    public static boolean isCodeCorrest(String str) throws PatternSyntaxException {
        String regExp = "^\\d{4}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(str);
        return m.matches();
    }

}
