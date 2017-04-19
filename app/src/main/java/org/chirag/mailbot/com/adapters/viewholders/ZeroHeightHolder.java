package org.chirag.mailbot.com.adapters.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import org.chirag.mailbot.com.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ZeroHeightHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.chatMessageView)
    public RelativeLayout chatMessage;

    public ZeroHeightHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
