package com.gabriel.activity;

import android.content.Intent;
import android.jb.barcode.BarcodeManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.widget.Toast;

import com.gabriel.R;
import com.gabriel.adapter.ViewPagerAdapter;
import com.gabriel.manager.MQTTManager;
import com.gabriel.util.Constant;
import com.gabriel.util.Logger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator off 2017/5/8/008.
 */
public class MainActivity extends FragmentActivity {
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAapter;
    private long firstTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        viewPager = (ViewPager) findViewById(R.id.vPager);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(DetectFragment.getInstance());
        fragments.add(new InquiryFragment());

        viewPagerAapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(viewPagerAapter);
        viewPager.setCurrentItem(0, true);

        DetectFragment.getInstance().setContext(this);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK : {
                long secondTime = System.currentTimeMillis();
                if (secondTime - firstTime > 2000) {    //如果两次按键时间间隔大于2秒，则不退出
                    Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;//更新firstTime
                    return true;
                } else {
                    String message = "quit";
                    Logger.D("发送程序退出的消息: " + message);
                    boolean sendResult = MQTTManager.getInstance().publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
                    Logger.I("发送程序退出的消息: " + sendResult);

                    //退出时取消订阅，释放资源
                    MqttClient client =  MQTTManager.getInstance().getClient();
                    if(client != null) {
                        try {
                            client.unsubscribe(Constant.TOPIC_SUBSCRIBE);
                            MQTTManager.release();
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);//两次按键小于2秒时，退出应用
                    BarcodeManager.getInstance().Barcode_Close();

                    System.exit(0);
                }
                break;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}
