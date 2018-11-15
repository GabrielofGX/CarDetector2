package com.gabriel.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.jb.barcode.BarcodeManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gabriel.R;
import com.gabriel.adapter.DetectListAdapter;
import com.gabriel.listener.DetailMessageListener;
import com.gabriel.listener.InquiryMessageListener;
import com.gabriel.manager.MQTTManager;
import com.gabriel.util.Constant;
import com.gabriel.util.DetectItem;
import com.gabriel.util.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 车辆检测
 * Created by Administrator off 2017/5/8/008.
 */
public class DetectFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener
                       ,AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener{
    private List<DetectItem> list = new ArrayList<>();
    private LinearLayout settingLayout;
    private ImageView topImageView;
    private DetectListAdapter detectAdapter;
    private ListView listView;
    private Button btn_startAndStop;
    private ToggleButton tb_power;
    private MQTTManager manager;
    private String carTypeContent;
    private String engineTypeContent;
    private EditText et_detect_num;
    private ProgressDialog dialog;
    private InquiryMessageListener inquiryListener;
    private DetailMessageListener detailListener;
    private static DetectFragment fragment = new DetectFragment();
    private boolean isPowerMsg = false;
    private int status = 0;  //0初始状态， 1进行中
    private Map<Integer, String> changeResult = new HashMap<>();  //临时保存的修改的检测结果
    private List<String> nullFlag = new ArrayList<>();
    private boolean isRecheck = false;
    private List<String> unClickId = new ArrayList<>();  //不可以点击的序号
    private List<Integer> index = new ArrayList<>();  //当前检测项的序号
    private boolean isPositiveButton = false;
    private boolean isPrint = true; //是否打印结果，默认为true
    private BarcodeManager barcodeManager;
    private Context mContext;

    public static DetectFragment getInstance(){
        if(fragment == null){
            fragment = new DetectFragment();
        }
        return fragment;
    }

    public void setContext(Context context){
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.I("DetectFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_detect, container, false);

        findAndSet(view);
        return view;
    }

    public void setInquiryListener(InquiryMessageListener listener){
        inquiryListener = listener;
    }
    public void setDetailListener(DetailMessageListener listener){
        detailListener = listener;
    }

    private void findAndSet(View view) {
        manager = MQTTManager.getInstance();
        manager.getClient().setCallback(new MyListener());
        manager.subscribe(Constant.TOPIC_SUBSCRIBE, 2);

        TextView tv = (TextView) view.findViewById(R.id.top_tv_title);
        tv.setText("车辆检测");
        topImageView = (ImageView) view.findViewById(R.id.top_iv);
        topImageView.setOnClickListener(this);
        settingLayout = (LinearLayout) view.findViewById(R.id.ll_setting);

        Spinner carType = (Spinner) view.findViewById(R.id.car_type);
        final ArrayAdapter<CharSequence> carTypeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.carType,
                android.R.layout.simple_spinner_dropdown_item);
        carType.setAdapter(carTypeAdapter);
        carType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                carTypeContent = "" + carTypeAdapter.getItem(position);
                Logger.I("carType: " + carTypeContent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Spinner engineType = (Spinner) view.findViewById(R.id.engine_type);
        final ArrayAdapter<CharSequence> engineTypeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.engineType,
                android.R.layout.simple_spinner_dropdown_item);
        engineType.setAdapter(engineTypeAdapter);
        engineType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                engineTypeContent = "" + engineTypeAdapter.getItem(position);
                Logger.I("engineType: " + engineTypeContent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        view.findViewById(R.id.btn_detect_confirm).setOnClickListener(this);
        tb_power = (ToggleButton) view.findViewById(R.id.tb_power);
        tb_power.setOnCheckedChangeListener(this);
        tb_power.setClickable(false);
        et_detect_num = (EditText)view.findViewById(R.id.et_detect_num);
//        Utils.disableShowSoftInput(et_detect_num);

        listView = (ListView) view.findViewById(R.id.detect_list_view);
        detectAdapter = new DetectListAdapter(list, getActivity());
        listView.setAdapter(detectAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        btn_startAndStop = (Button) view.findViewById(R.id.btn_start_stop);
        btn_startAndStop.setOnClickListener(this);
        view.findViewById(R.id.btn_ok).setOnClickListener(this);

        barcodeManager = BarcodeManager.getInstance();
        //  监听橙色按钮按键广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.jb.action.F4key");
        getActivity().registerReceiver(f4Receiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.top_iv :
                if(settingLayout.getVisibility() == View.GONE) {
                    settingLayout.setVisibility(View.VISIBLE);
                }else{
                    settingLayout.setVisibility(View.GONE);
                }
                break;
            case R.id.btn_detect_confirm :
                detectConfirm();
                break;
            case R.id.btn_start_stop :
                startAndStop();
                break;
            case R.id.btn_ok :
                if(!changeResult.isEmpty() && changeResult.containsValue("无")){
                    dialogShow("等待主控机确认结果...");
                    changeResult();
                }else {
                    sendManualDetectResultMessage();
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()){
            case R.id.tb_power :
                powerChange(isChecked);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DetectItem item = list.get(position);
        if(index.size() > 0){  //当前检测项不为空，则只能指定当前检测项结果
            if(index.contains(item.getId()) && item.getType() == 2){ //当前检测的手动检测项可以选择结果
                if(isRecheck){ //重检情况下
                    if(!unClickId.contains(String.valueOf(item.getId()))){ //如果当前项不在不能点击列表中，则可以修改结果
                        if (item.getResult().equals("合格")) {
                            item.setResult("不合格");
                        } else if (item.getResult().equals("不合格")) {
                            item.setResult("无");
                        } else {
                            item.setResult("合格");
                        }
                    }
                }else { //非重检，即正常情况下的手动检测项，点击选择结果
                    if (item.getResult().equals("合格")) {
                        item.setResult("不合格");
                    } else if (item.getResult().equals("不合格")) {
                        item.setResult("无");
                    } else {
                        item.setResult("合格");
                    }
                }
            }
        }else if(status == 1 && (list.get(list.size() -1).getResult().matches("不合格|无"))){ //检测完成，只保留不合格项
            if(item.getResult().equals("不合格")){
                item.setResult("无");
            }else {
                item.setResult("不合格");
            }
            changeResult.put(item.getId(), item.getResult());
        }
        detectAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        String result = list.get(list.size() -1).getResult();
        Logger.I("status: " + status + " ,result: " + result + " ,list.size(): " + list.size() + " ,item: " + list.get(list.size() -1).getItem());
        if(status == 1 && (result.matches("不合格|无"))) {
            recheckAlertDialogShow(position);
        }else if(status == 0){
            Toast.makeText(getActivity(), "检测还未开始~", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getActivity(), "检测未完成，不支持~", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * 打开或关闭电源
     * @param isChecked
     */
    private void powerChange(boolean isChecked){
        Logger.D("powerChanged: " + isChecked + " ,isPowerMsg: " + isPowerMsg);
        if(!isPowerMsg){
            String message;
            if(isChecked){
                //如果点击了取消，则直接返回
                powerOnDialog();
                if(!isPositiveButton) {
                    return;
                }
                message = "d1";   //发布电源打开的消息，消息内容是"d1"
            }else {
                message = "d0";   //发布电源关闭的消息，消息内容是"d0"
            }
            Logger.D("power on/off message: " + message);
            boolean result = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
            Logger.D("发送电源开关的消息结果: " + result);
        }else{
            isPowerMsg = false;
        }
    }

    /**
     * 打开电源开关的dialog
     */
    private void powerOnDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = View.inflate(getActivity(), R.layout.dialog_power_on, null);
        TextView tvCarType = (TextView) view.findViewById(R.id.dialog_power_on_carType);
        tvCarType.setText(carTypeContent);
        TextView tvEngineType = (TextView) view.findViewById(R.id.dialog_power_on_engineType);
        tvEngineType.setText(engineTypeContent);
        builder.setTitle("确定打开电源吗？");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isPositiveButton = true;
                myHandler.sendEmptyMessage(0);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isPositiveButton = false;
                myHandler.sendEmptyMessage(0);
            }
        });
        AlertDialog dialog =  builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(28);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(28);

        try{
            Looper.loop(); //阻塞主线程，等待用户点击确定或取消
        }catch (Exception e){

        }
    }
    private void detectConfirm(){
        if(tb_power.isChecked()){
            Toast.makeText(getActivity(), "电源已打开,不允许再次进行连接确认!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(status != 0){
            Toast.makeText(getActivity(), "检测进行中,不允许再次进行连接确认!", Toast.LENGTH_SHORT).show();
            return;
        }
        String carNum = et_detect_num.getText().toString().trim();
        if("".equals(carNum)){
            Toast.makeText(getActivity(), "请输入车辆编号!", Toast.LENGTH_SHORT).show();
        }else{
            dialogShow("检测连接确认中...");

            //发布车型检测确认的消息，消息内容是"b车型,发动机型号,车辆编号"
            String message = "b" + carTypeContent + " ," + engineTypeTransfer(engineTypeContent) + " ," + carNum;
            Logger.D("detect confirm message: " + message);
            boolean result = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
            Logger.D("发送车辆检测确认消息结果: " + result);
        }
    }
    //结束检测成功后发送打印结果的命令
    private void print(){
        //发布打印检测结果的消息，消息内容是"PRINT"
        String message = "PRINT";
        Logger.D("发送打印检测结果的消息: " + message);
        boolean result = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
        Logger.D("发送打印检测结果的消息: " + result);
    }

    class MyListener implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            Logger.E("Connection Lost: " + cause.toString());
            myHandler.sendEmptyMessage(2);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String msgGBK = new String(message.getPayload(), "GBK");
//            String msgGBK = new String(message.getPayload());
            Logger.I("messageArrived--- Topic: " + topic + " , message: " + msgGBK);
            if(Constant.TOPIC_SUBSCRIBE.equals(topic)){
                String msg = msgGBK.toString();
                Message messageArrived = Message.obtain();
                messageArrived.what = 1;
                messageArrived.obj = msg;
                myHandler.sendMessage(messageArrived);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    Handler myHandler = new Handler(){
        public void handleMessage(final Message msg) {
        switch (msg.what) {
            case 0:
                //打开电源时弹出AlertDialog，调用Looper.loop()阻塞主线程，点击确定或取消后发消息到Handler，抛出异常结束阻塞
                throw new RuntimeException();
            case 1: //处理分发主控机回复的消息
                handleMsg(msg.obj.toString());
                break;
            case 2:
                mqttReconnect();
                break;
        }
        super.handleMessage(msg);
        }
    };

    //分别处理接收到的消息
    private void handleMsg(String msg){
        switch (msg.substring(0,1)){
            case "b" :    //检测确认结果
                dialogDismiss();
                updateToggleButtonLine(msg);
                break;
            case "c" :    //获取检测项目结果
                dialogDismiss();
                updateListData(msg);
                break;
            case "d" :    //电源开关结果
                updateToggleButtonPower(msg);
                break;
            case "e" :    //开始检测结果
                updateStartAndStop(msg, 0);
                break;
            case "g" :    //停止检测结果
                updateStartAndStop(msg, 1);
                break;
            case "i" :    //PC发送的检测中自动检测项
                highLightCurrentDetectItem(msg, 1);
                break;
            case "j" :    //PC发送的检测中自动检测项检测结果
                updateAutoDetectResult(msg);
                break;
            case "k" :    //PC发送的检测中手动检测项
                highLightCurrentDetectItem(msg, 2);
                break;
            case "l" :   //PC发送手动检测项目结果回复
                manualDetectResultReply(msg);
                break;
            case "m" :   //更新修改检测结果界面
                updateChangeResultUI(msg);
                break;
            case "n" :   //更新重新检测界面
                updateRecheckUI(msg);
                break;
            case "t" :    //查询检测结果
                if(inquiryListener != null){
                    inquiryListener.handleMessage(msg);
                }else{
                    Logger.W("inquiryListener is null");
                }
                break;
            case "u" :    //查询所有项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询所有项目详情: detailListener is null");
                }
                break;
            case "v" :    //查询排除(无)项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询排除(无)项目详情: detailListener is null");
                }
                break;
            case "w" :    //查询合格项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询合格项目详情: detailListener is null");
                }
                break;
            case "x" :    //查询不合格项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询不合格项目详情: detailListener is null");
                }
                break;
            case "A" :    //查询已检项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询已检项目详情: detailListener is null");
                }
                break;
            case "z" : //查询未检项目详情
                if(detailListener != null){
                    detailListener.handleMessage(msg);
                }else{
                    Logger.W("查询未检项目详情: detailListener is null");
                }
                break;
        }
    }
    /**
     * MQTT断开后重新连接
     */
    private void mqttReconnect(){
        //TODO 未验证
        manager.startReconnect();
    }
    /**
     * 更新线束连接状态
     */
    private void updateToggleButtonLine(String msg){
        String content = msg.substring(1);
        Logger.D("更新线束连接状态 : " + content);
        if("0".equals(content)){  //线束连接正确
            tb_power.setClickable(true);
            getDetectItem();
        }else if("2".equals(content)){
            Toast.makeText(getActivity(), "车辆编号已存在，请更换！", Toast.LENGTH_SHORT).show();
            tb_power.setClickable(false);
        }else if("3".equals(content)){
            Toast.makeText(getActivity(), "车辆编号不符合规则，请检查！", Toast.LENGTH_SHORT).show();
            tb_power.setClickable(false);
        }else{
            Toast.makeText(getActivity(), "线束连接有问题，请检查！", Toast.LENGTH_SHORT).show();
            tb_power.setClickable(false);
        }
    }

    //收到检测项目后更新数据
    private void updateListData(String msg){
        String contents = msg.substring(2);
        Logger.I("检测项目: " + contents);
        String[] content = contents.split(",");
        for(int i=0; i<content.length; i++){
            DetectItem item = new DetectItem();
            item.setId(i+1);
            item.setItem(content[i]);
            list.add(item);
        }
        Logger.D("检测项目总数: " + list.size());
        detectAdapter.notifyDataSetChanged();
    }
    //更新电源开关状态
    private void updateToggleButtonPower(String msg){
        String content = msg.substring(1);
        Logger.D("电源开关结果: " + content);
        if("01".equals(content)){//电源关闭失败
            isPowerMsg = true;
            tb_power.setChecked(true);
            Toast.makeText(getActivity(), "电源关闭失败", Toast.LENGTH_SHORT).show();
        }else if("11".equals(content)){//电源打开失败
            isPowerMsg = true;
            tb_power.setChecked(false);
            Toast.makeText(getActivity(), "电源打开失败", Toast.LENGTH_SHORT).show();
        }
    }
   //更新开始检测和停止检测状态
    private void updateStartAndStop(String msg, int type){
        dialogDismiss();
        String content = msg.substring(1);
        Logger.D("收到主控机回复开始或停止的消息: " + content);
        if(type == 0) {    //开始检测
            if ("1".equals(content)) {
                Toast.makeText(getActivity(), "主控机回复开始检测执行失败，请检查！", Toast.LENGTH_SHORT).show();
            } else {
                status = 1;
                btn_startAndStop.setText("结束");
                tb_power.setClickable(false); //开始检测后，不可关闭或打开电源
            }
        }else{    //结束检测
            if ("1".equals(content)) {
                Toast.makeText(getActivity(), "主控机回复停止检测执行失败，请检查！", Toast.LENGTH_SHORT).show();
            } else {
                status = 0;
                btn_startAndStop.setText("开始");
                tb_power.setClickable(true);  //停止检测后，可关闭或打开电源
                if(isPrint) {
                    print();
                }
                reset();
            }
        }
    }

    //高亮显示检测中的项目, type=1表示自动检测项目，type=2表示手动检测项目
    private void highLightCurrentDetectItem(String msg, int type){
        index.clear();
        String content = msg.substring(1);
        Logger.D("当前检测项: " + content);
        List<String> name = new ArrayList<>();
        String[] contentArr = content.split(",");
        for(int i=0; i<contentArr.length; i++){
            Logger.I("detect item: " + contentArr[i]);
            if(i%2 == 0){     //偶数个
                index.add(praseToInt(contentArr[i]));
            }else{
                name.add(contentArr[i]);
            }
        }
        List<DetectItem> detectItems = new ArrayList<>();
        for(int i=0; i<index.size(); i++){
            DetectItem item = new DetectItem();
            item.setId(index.get(i));
            item.setItem(name.get(i));
            detectItems.add(item);
        }
        insertItemIfNotExists(detectItems);

        for(int inde : index){
            for(DetectItem detectItem : list){
                if(inde == detectItem.getId()){
                    Logger.D("检测中的序号: " + inde);
                    if(type == 1) {
                        detectItem.setResult("自动检测...");
                    }else{
                        if(detectItem.getResult() == null || "".equals(detectItem.getResult())) {
                            if(changeResult.containsKey(detectItem.getId())){
                                detectItem.setResult("不合格");
                            }else if( nullFlag.contains(String.valueOf(detectItem.getId()))){
                                detectItem.setResult("无");
                            } else{
                                detectItem.setResult("合格");
                            }
                        }
                        detectItem.setType(2);
                        if(detectItem.getResult().matches("无|合格") && isRecheck){
                            unClickId.add(String.valueOf(detectItem.getId()));
                        }
                    }
                    break;
                }
            }
        }
        detectAdapter.notifyDataSetChanged();
        //拿到检测项的编号在list中的位置
        int position =0;
        for(DetectItem item : list){
            if(item.getId() == index.get(0)){
                position = list.indexOf(item);
                break;
            }
        }
        listView.setSelection(position);
    }

    //list中不存在某个检测项，则插入
    private void insertItemIfNotExists(List<DetectItem> itemList){
        for(DetectItem newItem : itemList){
            boolean exists = false;
            for(DetectItem oldItem : list){
                if(newItem.getId() == oldItem.getId()){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                int insertPosition = -1;
                for(int i=0; i<list.size(); i++){
                    if(list.get(i).getId() < newItem.getId()){
                        if((i+1 < list.size()) && list.get(i+1).getId() > newItem.getId()) {
                            insertPosition = i + 1;
                            break;
                        }
                    }else{
                        insertPosition = 0;
                    }
                }
                if(insertPosition == -1){ // -1表示不在list中，直接往后插入
                    list.add(newItem);
                }else {
                    list.add(insertPosition, newItem);
                }
            }
        }
    }

    //主控机回复手动检测结果
    private void manualDetectResultReply(String message){
        String content = message.substring(1);
        Logger.D("主控机针对手动检测结果的回复: " + content);
        dialogDismiss();
        if("0".equals(content)){
            if(isRecheck){ //如果是重检
                unClickId.clear();
                isRecheck = false;
            }
            for(DetectItem item : list){
                if(item.getType() == 2){
                    item.setType(0);//无实际意义，用于标记此时该项目已经不是正在检测的手动项了
                }
            }
        }else{
            Toast.makeText(getActivity(), "主控机回复手动检测结果出错!", Toast.LENGTH_SHORT).show();
        }
        removeOKItem();
        index.clear();
    }
    //更新修改检测结果UI
    private void updateChangeResultUI(String message){
        String content = message.substring(1);
        Logger.D("主控机回复修改检测结果: " + content);
        if("0".equals(content)){
            removeOKItem();
        }else{
            Toast.makeText(getActivity(), "主控机回复修改检测结果出错!", Toast.LENGTH_SHORT).show();
        }
        dialogDismiss();
    }

    //更新重新检测结果UI
    private void updateRecheckUI(String message){
        String content = message.substring(1);
        Logger.D("主控机回复重新检测结果: " + content);
        if("0".equals(content)){
            isRecheck = true;
            Toast.makeText(getActivity(), "重新检测已准备好!", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getActivity(), "主控机回复重新检测准备出错!", Toast.LENGTH_SHORT).show();
        }
        dialogDismiss();
    }

    //更新自动检测结果UI
    private void updateAutoDetectResult(String message){
        String content = message.substring(1);
        Logger.D("检测结果: " + content);
        List<String> resultList = new ArrayList<>();
        String[] contentArr = content.split(",");
        for(int i=0; i<contentArr.length; i++){
            if(i%2 != 0){     //奇数个为结果，提取出来
                resultList.add(contentArr[i]);
            }
        }
        for(int i=0; i<index.size(); i++){
            for(DetectItem datectItem : list){
                if(index.get(i) == datectItem.getId()){
                    Logger.D("结果中的序号: " + index.get(i));
                    String result = resultList.get(i);
                    String chinese = resultTransferToChinese(result.trim());
                    datectItem.setResult(chinese);
                    break;
                }
            }
        }
        removeOKItem();
        if(index.size() > 0) {
            listView.setSelection(index.get(0));
        }
        index.clear();
    }
    //发送获取检测项目的消息
    private void getDetectItem(){
        dialogShow("获取待检测项目中...");

        //发布获取检测项目的消息，消息内容是"c"
        String message = "c";
        Logger.D("获取检测项目的消息: " + message);
        boolean result = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
        Logger.D("发送获取检测项目消息结果: " + result);

        list.clear();
    }
    //发送修改检测结果
    private void changeResult(){
        for(int id : changeResult.keySet()) {
            if("无".equals(changeResult.get(id))) {
                String message = "m" + id + " " + resultTransferToChar(changeResult.get(id));
                Logger.D("发送修改检测结果的消息: " + message);
                boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
                Logger.D("发送修改检测结果的消息: " + sendResult);
                if(sendResult){
                    nullFlag.add(String.valueOf(id));
                }
            }
        }
        changeResult.clear();
    }
    //发送手动检测项目的结果
    private void sendManualDetectResultMessage(){
        List<String> indexList = new ArrayList<>();
        List<String> resultList = new ArrayList<>();
        for(DetectItem item : list){
            if(item.getType() == 2){
                indexList.add(String.valueOf(item.getId()));
                resultList.add(item.getResult());
            }
        }
        if(indexList.size() == 0){
            return;
        }

        String message = "l";
        for(int i=0; i<indexList.size(); i++){
            Logger.D("index: " + indexList.get(i));
            if(message.endsWith("l")) {
                message += (indexList.get(i) + " ," + resultTransferToChar(resultList.get(i).trim()));
            }else{
                message += (" ," + indexList.get(i) + " ," + resultTransferToChar(resultList.get(i).trim()));
            }
        }
        //手动检测项结果，消息内容为"l1 ,Y ,2 ,Y..."、"l1 ,N ,2 ,N..."、"l1 ,E ,2 ,E..."
        Logger.D("手动检测结果的消息: " + message);
        boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
        Logger.D("发送手动检测结果的消息: " + sendResult);

        dialogShow("等待主控机确认中...");
    }

    //发送重检或修改检测结果的消息, content "Y"表示"合格"...   type = 1 表示重检, type = 2 表示修改结果
    private void sendRecheckOrChangeResultMessage(int position, String content, int type){
        Logger.D("重检或修改检测结果的项目：" + position + " ,content = " + content + " , type = " + type);
        String message;
        if(type == 1){
            message = "n" + position;
        }else{
            message = "m" + position + " ," + content;
        }
        Logger.D("发送重检或修改检测结果的消息: " + message);
        boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
        Logger.D("发送重检或修改检测结果的消息: " + sendResult);

    }
    private void startAndStop(){
        if(tb_power.isChecked()) {
            if (status == 1) {
                showAlertDialog("");
            } else {
                dialogShow("开始检测确认中...");
                String message = "e";
                Logger.D("发送开始检测的消息: " + message);
                boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
                Logger.D("发送开始检测的消息: " + sendResult);
            }
        }else if(settingLayout.getVisibility() == View.GONE){
            Toast.makeText(getActivity(), "请先点击右上角+进行配置", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getActivity(), "请先进行配置并保证电源处于开启状态", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAlertDialog(String detectResult){
        TextView title = new TextView(getActivity());
        title.setTextSize(30);
        title.setPadding(20, 20,1,20);//左、上、右、下
        title.setTextColor(ContextCompat.getColor(getActivity(), R.color.alertDialogTextColor));
        title.setText("确定结束检测，并打印结果吗？");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(!"".equals(detectResult)) {
            builder.setMessage(generateDetectResult(detectResult));
        }
        builder.setCustomTitle(title)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String message = "g";
                        Logger.D("发送结束检测的消息: " + message);
                        boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
                        Logger.D("发送结束检测的消息: " + sendResult);
                        dialogShow("结束检测确认中...");
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isPrint = false;
                        String message = "g";
                        Logger.D("取消--发送结束检测的消息: " + message);
                        boolean sendResult = manager.publish(Constant.TOPIC_PUBLISH, 2, message.getBytes());
                        Logger.D("取消--发送结束检测的消息: " + sendResult);
                        dialogShow("结束检测确认中...");
//                        dialogDismiss();
                    }
                });
        AlertDialog dialog =  builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(28);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(28);
    }

    //将查询的检测结果组装成展示的格式
    private String generateDetectResult(String message){
        String result = "";
        if(message != null && !"".equals(message)) {
            String[] msgs = message.split(",");
            if(msgs.length == 8){
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("   检测统计        数量" + "\n");
                stringBuilder.append("   所有项目        " + msgs[2] + "\n");
                stringBuilder.append("   排除项目        " + msgs[3] + "\n");
                stringBuilder.append("   已检项目        " + msgs[4] + "\n");
                stringBuilder.append("   未检项目        " + msgs[5] + "\n");
                stringBuilder.append("   合格项目        " + msgs[6] + "\n");
                stringBuilder.append("   不合格项目      " + msgs[7] + "\n\n");
                stringBuilder.append("是否打印检测结果？");
                result = stringBuilder.toString();
            }
        }else{
            Logger.W("generateDetectResult message is empty: " + message);
        }
        return result;
    }

    //删除合格项或者结果为"无"
    private void removeOKItem(){
        Iterator<DetectItem> iterator = list.iterator();
        while(iterator.hasNext()){
            DetectItem item = iterator.next();
            if(item.getResult().matches("合格|无")){
                iterator.remove();
            }
        }
        detectAdapter.notifyDataSetChanged();
    }

    //结束检测后恢复到初始化状态
    private void reset(){
        list.clear();
        detectAdapter.notifyDataSetChanged();

        tb_power.setChecked(false);
        tb_power.setClickable(false);
        et_detect_num.setText("");
        isPowerMsg = false;
        status = 0;
        nullFlag.clear();
        isRecheck = false;
        isPositiveButton = false;
        unClickId.clear();
        changeResult.clear();
        index.clear();
        isPrint = true;
    }





    //结果中的"Y"、"N"、"E"转化为"合格" 、"不合格"、 "排除(无)项"
    private String resultTransferToChinese(String str){
        switch(str){
            case "Y" :
                return "合格";
            case "N" :
                return "不合格";
            case "E" :
                return "无";
            default :
                return "未知结果";
        }
    }
    //结果中的"合格" 、"不合格"、 "无" 转化为"Y"、"N"、"E"
    private String resultTransferToChar(String str){
        switch(str){
            case "合格" :
                return "Y";
            case "不合格" :
                return "N";
            case "无" :
                return "E";
            default :
                return "未知结果";
        }
    }
    //发动机型号转为编号
    private String engineTypeTransfer(String str){
        switch(str){
            case "欧Ⅱ" :
                return "0";
            case "欧Ⅲ" :
                return "1";
            case "国Ⅲ" :
                return "2";
            case "国Ⅳ" :
                return "3";
            case "国Ⅴ" :
                return "4";
            case "LNG" :
                return "5";
            case "WP13" :
                return "6";
            case "风冷" :
                return "7";
            default :
                return "未知结果";
        }
    }

    //显示重检的dialog
    private void recheckAlertDialogShow(final int position){
        if(!list.get(position).getResult().matches("合格|不合格|无")){
            Toast.makeText(getActivity(), "当前状态不支持重检或修改结果!", Toast.LENGTH_SHORT).show();
            return;
        }
        TextView title = new TextView(getActivity());
        title.setTextSize(25);
        title.setPadding(20,20,1,20);//左、上、右、下
        title.setTextColor(ContextCompat.getColor(getActivity(), R.color.alertDialogTextColor));
        title.setText("确定重检吗？");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(title);
        builder.setMessage(list.get(position).getId() + "  " + list.get(position).getItem());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendRecheckOrChangeResultMessage(list.get(position).getId(), "", 1);
                dialogShow("等待主控机确认中...");
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog =  builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setTextSize(25);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(28);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(28);
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

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Logger.I("detect setUserVisibleHint " + isVisibleToUser);
        if(isVisibleToUser){
            if(barcodeManager == null) {
                barcodeManager = BarcodeManager.getInstance();
            }
            barcodeManager.Barcode_Open(mContext, dataReceived);
        } else {
            if(barcodeManager != null) {
                barcodeManager.Barcode_Close();
            }
        }
    }

    BarcodeManager.Callback dataReceived = new BarcodeManager.Callback() {
        @Override
        public void Barcode_Read(byte[] buffer, String codeId, int errorCode) {
            String result = new String(buffer);
            Logger.I("barcode scan result: " + result + " ,code: " + codeId + " ,i=" + errorCode);
            barcodeManager.Barcode_Stop();
            et_detect_num.setText(result);
            et_detect_num.setSelection(et_detect_num.getText().toString().trim().length());
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
                    Logger.D("key down");
                    if (null != barcodeManager) {
                        barcodeManager.Barcode_Stop();
                        if (null != barcodeManager) {
                            barcodeManager.Barcode_Start();
                        }
                    }
                } else if (intent.getStringExtra("F4key").equals("up")) {
                    Logger.D("key up");
                }
            }
        }
    };


    /**
     * string 转化为int
     * @param str
     * @return
     */
    private int praseToInt(String str){
        int value = -1;
        if(str != null && !"".equals(str)){
            try{
                value = Integer.parseInt(str);
            }catch (Exception e){
                Logger.W("praseToInt exception: " + str);
            }
        }
        return  value;
    }
}