package com.gabriel.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.jb.barcode.BarcodeManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gabriel.R;
import com.gabriel.adapter.InquiryListAdapter;
import com.gabriel.listener.InquiryMessageListener;
import com.gabriel.manager.MQTTManager;
import com.gabriel.util.Constant;
import com.gabriel.util.Logger;
import com.gabriel.util.Utils;

/**
 * Created by Administrator off 2017/5/8/008.
 */
public class InquiryFragment extends Fragment implements View.OnClickListener{
    private ListView listView;
    private InquiryListAdapter adapter;
    private EditText carNum;
    private TextView carType, engineType;
    private MQTTManager manager;
    private ProgressDialog dialog;
    private String[] num;
    private String[] item;
    private String[] text;
    private BarcodeManager barcodeManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.I("InquiryFragment onCreateView");

        View view = inflater.inflate(R.layout.fragment_inquiry, container, false);
        findAndSet(view);
        return view;
    }

    private void findAndSet(View view){
        TextView tv = (TextView) view.findViewById(R.id.top_tv_title);
        tv.setText("查询结果");

        view.findViewById(R.id.top_iv).setVisibility(View.GONE);//设置标题栏+不显示
        listView = (ListView) view.findViewById(R.id.listview);

        item = new String[]{"检测统计", "所有项目", "排除项目", "已检项目", "未检项目","合格项目", "不合格项目"};
        num = new String[]{"数量", "0", "0", "0", "0", "0", "0"};
        text = new String[]{"详情", "查看详情", "查看详情", "查看详情", "查看详情", "查看详情", "查看详情"};
        adapter = new InquiryListAdapter(item, num, text, getActivity());
        listView.setAdapter(adapter);

        view.findViewById(R.id.btn_inquiry).setOnClickListener(this);
        carNum = (EditText)view.findViewById(R.id.et_inquiry_num);
        Utils.disableShowSoftInput(carNum);
        carType = (TextView)view.findViewById(R.id.tv_car_type);
        engineType = (TextView)view.findViewById(R.id.tv_engine_type);

        manager = MQTTManager.getInstance();
        DetectFragment.getInstance().setInquiryListener(new MessageListener());

        barcodeManager = BarcodeManager.getInstance();
        //  监听橙色按钮按键广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.jb.action.F4key");
        getActivity().registerReceiver(f4Receiver, intentFilter);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Logger.I("inquery setUserVisibleHint " + isVisibleToUser);
        if(isVisibleToUser){
            if(barcodeManager == null) {
                barcodeManager = BarcodeManager.getInstance();
            }
            barcodeManager.Barcode_Open(getActivity(), dataReceived);
        } else {
            if(barcodeManager != null) {
                barcodeManager.Barcode_Close();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_inquiry :
                inquiry();
                dialogShow("正在查询中...");
                break;
        }
    }

    private void inquiry(){
        String car = carNum.getText().toString().trim();
        if("".equals(car)){
            Toast.makeText(getActivity(), "请输入车辆编号!", Toast.LENGTH_SHORT).show();
        }else{
            String message = "t" + car;    //发布查询结果的消息，消息内容是"t车辆编号"
            Logger.D("inquiry result message: " + message);
            boolean result = manager.publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
            Logger.I("发送查询检测结果的消息结果: " + result);
        }
    }

    class MessageListener implements InquiryMessageListener{
        @Override
        public void handleMessage(String message) {
            Logger.I("InquiryMessageListener: " + message);
            if(message.startsWith("t")){
                dialogDismiss();
                String msg = message.substring(1);
                String[] car = msg.split(",");
                if(car.length == 8) {
                    carType.setText(car[0]);
                    engineType.setText(car[1]);
                    num = new String[]{"数量", car[2], car[3], car[4], car[5], car[6], car[7]};
                    adapter = new InquiryListAdapter(item, num, text, getActivity());
                    listView.setAdapter(adapter);
                }else{
                    Toast.makeText(getActivity(), "无当前编号", Toast.LENGTH_SHORT).show();
                    Logger.E("主控机返回信息长度不是8个.");
                }
            }
        }
    }

    private void dialogShow(String message){
        if(dialog == null) {
            dialog = new ProgressDialog(getActivity());
        }
        dialog.setMessage(message);
        dialog.show();
    }

    private void dialogDismiss(){
        if(dialog != null){
            dialog.dismiss();
        }
    }

    BarcodeManager.Callback dataReceived = new BarcodeManager.Callback() {
        @Override
        public void Barcode_Read(byte[] buffer, String codeId, int errorCode) {
            String result = new String(buffer);
            Logger.I("inquiry result: " + result + " ,code: " + codeId + " ,i=" + errorCode);
            barcodeManager.Barcode_Stop();
            carNum.setText(result);
            carNum.setSelection(carNum.getText().toString().trim().length());
        }
    };

    /**
     * 捕获扫描物理按键广播
     */
    private BroadcastReceiver f4Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("F4key")) {
                if (intent.getStringExtra("F4key").equals("down")) {
                    Logger.I("key down");
                    if (null != barcodeManager) {
                        barcodeManager.Barcode_Stop();
                        if (null != barcodeManager) {
                            barcodeManager.Barcode_Start();
                        }
                    }
                } else if (intent.getStringExtra("F4key").equals("up")) {
                    Logger.I("key up");
                }
            }
        }
    };
}
