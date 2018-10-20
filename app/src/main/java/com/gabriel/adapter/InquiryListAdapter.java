package com.gabriel.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gabriel.R;
import com.gabriel.activity.DetectFragment;
import com.gabriel.listener.DetailMessageListener;
import com.gabriel.manager.MQTTManager;
import com.gabriel.util.Constant;
import com.gabriel.util.DetailItem;
import com.gabriel.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator off 2017/5/11/011.
 */
public class InquiryListAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private ViewHolder viewHolder;
    private String[] item;
    private String[] num;
    private String[] text;
    private Context context;
    private ProgressDialog dialog;

    public InquiryListAdapter(String[] item, String[] num, String[] text, Context context){
        this.item = item;
        this.num = num;
        this.text = text;
        this.context = context;

        DetectFragment.getInstance().setDetailListener(new MessageListener());
    }

    @Override
    public int getCount() {
        return num.length;
    }

    @Override
    public Object getItem(int position) {
        return num[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        Logger.I("position ==  " + position);
        if (view == null) {
            Logger.I("inquiry view == null ");
            viewHolder = new ViewHolder();
            mLayoutInflater = LayoutInflater.from(context);
            view = mLayoutInflater.inflate(R.layout.inquiry_list_item, null);

            viewHolder.item = (TextView) view.findViewById(R.id.tv_inquiry_list_item);
            viewHolder.num = (TextView) view.findViewById(R.id.tv_inquiry_list_num);
            viewHolder.detail = (TextView) view.findViewById(R.id.tv_inquiry_list_detail);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        //标题栏字体黑体
        if(item[position].equals("检测统计")){
            viewHolder.item.setTextColor(context.getResources().getColor(R.color.black));
            viewHolder.num.setTextColor(context.getResources().getColor(R.color.black));
            viewHolder.detail.setTextColor(context.getResources().getColor(R.color.black));
            viewHolder.detail.setClickable(false);
        }else {
            viewHolder.item.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
            viewHolder.num.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
            viewHolder.detail.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
            viewHolder.detail.setClickable(true);
        }
        if(item[position].equals("不合格项目")){
            viewHolder.item.setTextColor(context.getResources().getColor(R.color.red));
            viewHolder.num.setTextColor(context.getResources().getColor(R.color.red));
            viewHolder.detail.setTextColor(context.getResources().getColor(R.color.red));
        }else{
            viewHolder.item.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
            viewHolder.num.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
            viewHolder.detail.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
        }
        viewHolder.item.setText(item[position]);
        viewHolder.num.setText(num[position]);
        viewHolder.detail.setText(text[position]);
        viewHolder.detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "查看" + item[position] + "的详细信息", Toast.LENGTH_SHORT).show();

                inquiryMessage(item[position]);
                dialogShow("获取数据中...");
            }
        });
        return view;
    }

    //发送查询详细情况消息
    private void inquiryMessage(String type){
        String message = "";
        switch(type){
            case "所有项目" :
                message = "u";
                break;
            case "排除项目" :
                message = "v";
                break;
            case "已检项目" :
                message = "A";
                break;
            case "未检项目" :
                message = "z";
                break;
            case "合格项目" :
                message = "w";
                break;
            case "不合格项目" :
                message = "x";
                break;
        }

        Logger.D("inquiry detail message: " + message);
        boolean result = MQTTManager.getInstance().publish(Constant.TOPIC_PUBLISH, 0, message.getBytes());
        Logger.I("发送查询详细情况的消息结果: " + result);
    }

    //弹出详情列表
    private void dialogShowDetail(List<DetailItem> list, String title){
        View view = LayoutInflater.from(context).inflate(R.layout.detail_list, null);
        ListView listView = (ListView) view.findViewById(R.id.detail_list_view);

        DetailListAdapter adapter = new DetailListAdapter(list, context);
        listView.setAdapter(adapter);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setTitle(title + "详细情况");
        builder.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }


    class MessageListener implements DetailMessageListener {
        @Override
        public void handleMessage(String message) {
            Logger.I("DetailMessageListener: " + message);
            String msgType = message.substring(0,1);
            String title = "";
            dialogDismiss();

            List<DetailItem> list = new ArrayList<>();
            switch (msgType){
                case "u" :    //查询所有项目详情
                    list = messageResolve(message.substring(1).trim(), 0);
                    title = "所有项目";
                    break;
                case "v" :    //查询排除项目详情
                    list = messageResolve(message.substring(1).trim(), 1);
                    title = "排除项目";
                    break;
                case "w" :    //查询合格项目详情
                    list = messageResolve(message.substring(1).trim(), 2);
                    title = "合格项目";
                    break;
                case "x" :    //查询不合格项目详情
                    list = messageResolve(message.substring(1).trim(), 3);
                    title = "不合格项目";
                    break;
                case "A" :    //查询已检项目详情
                    list = messageResolve(message.substring(1).trim(), 4);
                    title = "已检项目";
                    break;
                case "z" :   //查询未检项目详情
                    list = messageResolve(message.substring(1).trim(), 5);
                    title = "未检项目";
                    break;
            }
            if(list.size() > 0) {
                dialogShowDetail(list, title);
            }
        }
    }

    //消息内容解析,type=0表示所有项，1表示排除项，2表示合格项，3表示不合格项，4表示已检，5表示未检
    private List<DetailItem> messageResolve(String message, int type){
        Logger.I("messageResolve: " + message);
        List<DetailItem> list = new ArrayList<>();

        String[] array = message.split(",");
        if(array != null && array.length < 2){  //如果返回消息不包含项目，则直接返回list(list里面内容为空)
            return list;
        }

        if(type == 0){
            for(int i=0; i<array.length; ){
                DetailItem item = new DetailItem(array[i], array[i+1], array[i+2]);
                list.add(item);
                i+=3;
            }
        }else{
            String result = transferToChinese(type);

            for(int i=0; i<array.length; ){
                DetailItem item = new DetailItem(array[i], array[i+1], result);
                list.add(item);
                i+=2;
            }
        }
        return list;
    }

    //根据检测项目消息类型转化结果
    private String transferToChinese(int type){
        switch(type){
            case 1 :
                return "排除项";
            case 2 :
                return "合格";
            case 3 :
                return "不合格";
            case 4 :
                return "已检";
            case 5 :
                return "未检";
            default :
                return "未知结果";
        }
    }
    private void dialogShow(String message){
        if(dialog == null) {
            dialog = new ProgressDialog(context);
        }
        dialog.setMessage(message);
        dialog.show();
    }

    private void dialogDismiss(){
        if(dialog != null){
            dialog.dismiss();
        }
    }

    class ViewHolder {
        TextView item;
        TextView num;
        TextView detail;
    }
}



