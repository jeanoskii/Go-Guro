package com.upou.jeano.uber.HistoryRecyclerView;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.upou.jeano.uber.R;

/**
 * Created by Jeano on 16/03/2018.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView rideId;

    public HistoryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        rideId = (TextView) itemView.findViewById(R.id.rideId);
    }

    @Override
    public void onClick(View view) {

    }
}
