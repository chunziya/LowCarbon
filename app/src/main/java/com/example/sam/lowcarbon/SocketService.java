package com.example.sam.lowcarbon;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;

public class SocketService extends Service {

    private Socket client;
    private DataOutputStream out;
    private DataInputStream in; 

    public SocketService() throws IOException {
    }

    public void sendSocket(final String msg) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (client == null) {
                    try {
                        client = new Socket(Constant.SOCKET_IP, Constant.SOCKET_PORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (client != null && client.isConnected()) {
                    try {
                        out.writeUTF(msg);
                        out.flush();
                        Log.i("socketsend", msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null) {
                        client = new Socket(Constant.SOCKET_IP, Constant.SOCKET_PORT);
                    }
                    Log.i("socket", "start");
                    if (client != null && client.isConnected()) {
                        in = new DataInputStream(client.getInputStream());
                        out = new DataOutputStream(client.getOutputStream());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    Log.i("socket", "inputstream");
                                    try {
                                        String response = in.readUTF();
                                        JSONObject jsonObject = new JSONObject(response);
                                        int method = jsonObject.getInt("method");
                                        switch (method) {
                                            case Constant.SOCKET_FRIEND_ADD: {
                                                String from = jsonObject.getString("from");
                                                String to = jsonObject.getString("to");
                                                Intent intent = new Intent();
                                                intent.putExtra("from", from);
                                                intent.putExtra("to", to);
                                                intent.setAction("com.example.sam.lowcarbon.friend");
                                                sendBroadcast(intent);
                                                break;
                                            }
                                            default:
                                                break;
                                        }
                                        Log.i("socket request", response);
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class MyBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (client != null && client.isConnected()) {
            try {
                client.close();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
