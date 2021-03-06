package com.upou.jeano.goguro.HistoryRecyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.upou.jeano.goguro.R;

import java.util.List;

/**
 * Created by Jeano on 16/03/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
    private List<HistoryObject> itemList;
    private Context context;
    public HistoryAdapter(List<HistoryObject> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }
    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolder rcv = new HistoryViewHolder(layoutView);
        return rcv;
    }
    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.rideId.setText(itemList.get(position).getRideId());
        holder.time.setText(itemList.get(position).getTime());
        holder.name.setText(itemList.get(position).getName());
    }
    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}
