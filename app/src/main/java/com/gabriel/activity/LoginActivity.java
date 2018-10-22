package com.gabriel.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.gabriel.R;
import com.gabriel.manager.MQTTManager;
import com.gabriel.util.Constant;
import com.gabriel.util.Logger;
import com.gabriel.util.Utils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Administrator off 2017/5/10/010.
 */
public class LoginActivity extends Activity implements View.OnClickListener {
    private EditText et_account, et_password;
    private ImageButton ib_password_show;
    private boolean isConnected = false;
    private MQTTManager manager;
    ProgressDialog dialog;
    private String ip = "192.168.1.10";
//    private String ip = "iot.eclipse.org";
//    private String ip = "172.28.25.154";
    private String port = "1883";
    private int isShow = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        connectToServer();//主动连接消息服务器

        Logger.deleteLogFile(); //删除旧的日志文件
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login: {
                login();
                break;
            }
            case R.id.ib_password_show: {
                if(isShow == 0) {
                    et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ib_password_show.setBackground(getResources().getDrawable(R.mipmap.hide));
                    et_password.setSelection(et_password.getText().toString().length());
                    isShow = 1;
                }else{
                    et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ib_password_show.setBackground(getResources().getDrawable(R.mipmap.show));
                    et_password.setSelection(et_password.getText().toString().length());
                    isShow = 0;
                }
                break;
            }
        }
    }

    private void connectToServer() {
        String url = "tcp://" + ip + ":" + port;
        dialogShow("正在连接...");
        asynConnect(url);
    }

    private void login() {
        String account = et_account.getText().toString().trim();
        String password = et_password.getText().toString().trim();
        if (!isConnected) {
            Toast.makeText(this, "请先连接后再尝试登录!", Toast.LENGTH_SHORT).show();
        } else if ("".equals(account) || "".equals(password)) {
            Toast.makeText(this, "请输入登录账号和密码!", Toast.LENGTH_SHORT).show();
        } else {
            dialogShow("登录中...");

            //发布关于登录话题的消息，消息内容是"a+account+password"
            String message = "a" + account + " ," + password;
            Logger.D("login message: " + message);
            boolean result = manager.publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
            Logger.I("发送登录消息结果: " + result);
        }
    }


    //获取汽车型号信息
    private void getCarType() {
        //发布获取汽车型号的消息，消息内容是"B"
        String message = "B";
        Logger.D("get carType message: " + message);
        boolean result = manager.publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
        Logger.I("发送获取汽车型号的消息结果: " + result);
    }

    //获取发动机型号信息
    private void getEngineType() {
        //发布获取发动机型号的消息，消息内容是"C"
        String message = "C";
        Logger.D("get engineType message: " + message);
        boolean result = manager.publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
        Logger.I("发送获取发动机型号的消息结果: " + result);
    }

    private void asynConnect(final String url) {
        final String imei  = Utils.getDeviceIMEI(this);
        Logger.I("imei: " + imei);
        new Thread(new Runnable() {
            @Override
            public void run() {
                isConnected = manager.creatConnect(url, null, null, null);
//                isConnected = manager.creatConnect(url, "admin", "admin1234", null);
                Message msg = Message.obtain();
                msg.what = 0;
                msg.arg1 = isConnected ? 1 : 0;
                myHandler.handleMessage(msg);
            }
        }).start();
    }

    Handler myHandler = new Handler() {
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case 0:
                    dialogDismiss();
                    manager.subscribe(Constant.TOPIC_SUBSCRIBE, 0);
                    manager.getClient().setCallback(new MyListener());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int isConnected = msg.arg1;
                            if (isConnected == 1) {
                                Toast.makeText(LoginActivity.this, "连接服务器成功!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "连接服务器失败!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
                case 1:
                    if ("0".equals(msg.obj.toString())) {
                        Toast.makeText(LoginActivity.this, "身份验证成功!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else if ("1".equals(msg.obj.toString())) {
                        Toast.makeText(LoginActivity.this, "账号不存在!", Toast.LENGTH_SHORT).show();
                    } else if ("2".equals(msg.obj.toString())) {
                        Toast.makeText(LoginActivity.this, "密码错误!", Toast.LENGTH_SHORT).show();
                    } else if ("3".equals(msg.obj.toString())) {
                        Toast.makeText(LoginActivity.this, "主控机未准备好，请稍候!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "主控机应答错误，未知异常!", Toast.LENGTH_SHORT).show();
                    }
                    dialogDismiss();
                    break;
                case 2:
                    dealwithMessage(msg.obj.toString());
                    break;

            }
            super.handleMessage(msg);
        }
    };

    private void dealwithMessage(String message) {
        Logger.I("dealwithMessage: " + message);
        if (message.startsWith("B")) {

        }
    }

    class MyListener implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            Logger.E("Connection Lost: " + cause.toString());
            isConnected = false;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String msgGBK = new String(message.getPayload(), "GBK");
            Logger.I("messageArrived--- Topic: " + topic + " , message: " + msgGBK.toString());
            if (Constant.TOPIC_SUBSCRIBE.equals(topic)) {
                String msg = msgGBK.toString();
                Message messageArrived = Message.obtain();
                if (msg.startsWith("a")) {
                    messageArrived.what = 1;
                    messageArrived.obj = msg.substring(1);
                    Logger.I("messageBody: " + msg.substring(1));
                } else {
                    messageArrived.what = 2;
                    messageArrived.obj = msg;
                    Logger.I("messageBody: " + msg.substring(1));
                }
                myHandler.sendMessage(messageArrived);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    private void initView() {
        et_account = (EditText) findViewById(R.id.et_account);
        et_password = (EditText) findViewById(R.id.et_password);
//        Utils.disableShowSoftInput(et_account);
//        Utils.disableShowSoftInput(et_password);

        findViewById(R.id.btn_login).setOnClickListener(this);
        ib_password_show = (ImageButton) findViewById(R.id.ib_password_show);
        ib_password_show.setOnClickListener(this);

        manager = MQTTManager.getInstance();
        dialog = new ProgressDialog(this);
    }

    private void dialogShow(String message){
        if(dialog == null) {
            dialog = new ProgressDialog(this);
        }
        dialog.setMessage(message);
        dialog.show();
    }

    private void dialogDismiss(){
        if(dialog != null){
            dialog.dismiss();
        }
    }
}
