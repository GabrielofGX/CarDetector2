package com.gabriel.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gabriel.R;
import com.gabriel.util.DetailItem;
import com.gabriel.util.Logger;

import java.util.List;

/**
 * Created by Administrator off 2017/5/11/011.
 */
public class DetailListAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private ViewHolder viewHolder;
    private List<DetailItem> list;
    private Context context;

    public DetailListAdapter(List<DetailItem> list, Context context){
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
        Logger.I("detailAdapter position ==  " + position);
        if (view == null) {
            viewHolder = new ViewHolder();
            mLayoutInflater = LayoutInflater.from(context);
            view = mLayoutInflater.inflate(R.layout.detail_list_item, null);

            viewHolder.index = (TextView) view.findViewById(R.id.detail_item_index);
            viewHolder.item = (TextView) view.findViewById(R.id.detail_item_item);
            viewHolder.result = (TextView) view.findViewById(R.id.detail_item_result);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.index.setText(String.valueOf(list.get(position).getId()));
        viewHolder.item.setText(list.get(position).getItem());
        viewHolder.result.setText(list.get(position).getResult());
        return view;
    }

    class ViewHolder {
        TextView index;
        TextView item;
        TextView result;
    }
}



