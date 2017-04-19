package org.chirag.mailbot.com.adapters.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import org.chirag.mailbot.com.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.tajchert.sample.DotsTextView;

public class TypingDotsHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.dots)
    public DotsTextView dotsTextView;
    @BindView(R.id.background_layout)
    public LinearLayout backgroundLayout;

    public TypingDotsHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
