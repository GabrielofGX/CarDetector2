package com.gabriel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gabriel.R;
import com.gabriel.util.DetectItem;
import com.gabriel.util.Logger;

import java.util.List;

/**
 * Created by Administrator off 2017/5/11/011.
 */
public class DetectListAdapter extends BaseAdapter{
    private LayoutInflater mLayoutInflater;
    private ViewHolder viewHolder;
    private List<DetectItem> list;
    private Context context;

    public DetectListAdapter(List<DetectItem> list, Context context){
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            viewHolder = new ViewHolder();
            mLayoutInflater = LayoutInflater.from(context);
            view = mLayoutInflater.inflate(R.layout.detect_list_item, null);

            viewHolder.id = (TextView) view.findViewById(R.id.tv_detect_list_id);
            viewHolder.item = (TextView) view.findViewById(R.id.tv_detect_list_item);
            viewHolder.result = (TextView) view.findViewById(R.id.tv_detect_list_result);
            viewHolder.ll_listItem = (LinearLayout) view.findViewById(R.id.ll_list_item);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }


        viewHolder.id.setText(String.valueOf(list.get(position).getId()));
        viewHolder.item.setText(list.get(position).getItem());
        viewHolder.result.setText(list.get(position).getResult());
        if(list.get(position).getResult().equals("不合格")){
            viewHolder.result.setTextColor(context.getResources().getColor(R.color.red));
        }else{
            viewHolder.result.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
        }

        if(list.get(position).getResult().contains("检测") || list.get(position).getType() == 2){
            viewHolder.ll_listItem.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        }else{
            viewHolder.ll_listItem.setBackgroundColor(context.getResources().getColor(R.color.white));
        }
        return view;
    }

    class ViewHolder {
        TextView id;
        TextView item;
        TextView result;
        LinearLayout ll_listItem;
    }
}



