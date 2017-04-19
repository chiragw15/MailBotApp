package org.chirag.mailbot.com.adapters.viewholders;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.chirag.mailbot.com.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.chirag.mailbot.com.adapters.recycleradapters.ChatFeedRecyclerAdapter.BOT_MESSAGE;
import static org.chirag.mailbot.com.adapters.recycleradapters.ChatFeedRecyclerAdapter.USER_MESSAGE;

public class ChatViewHolder extends MessageViewHolder{

    @BindView(R.id.text)
    public TextView chatTextView;
    @BindView(R.id.timestamp)
    public TextView timeStamp;
    @BindView(R.id.background_layout)
    public LinearLayout backgroundLayout;
    @Nullable @BindView(R.id.received_tick)
    public ImageView receivedTick;

    public ChatViewHolder(View view, ClickListener clickListener ,int myMessage) {
        super(view,clickListener);
        ButterKnife.bind(this, view);
        switch (myMessage) {
            case USER_MESSAGE:
                break;
            case BOT_MESSAGE:
                break;
            default:
        }
    }
}