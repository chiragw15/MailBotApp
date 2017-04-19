package org.chirag.mailbot.com.adapters.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.chirag.mailbot.com.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DateViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.date)
    public TextView textDate;

    public DateViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
