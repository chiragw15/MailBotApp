package org.chirag.mailbot.com.adapters.recycleradapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.RequestManager;
import org.chirag.mailbot.com.R;
import org.chirag.mailbot.com.adapters.viewholders.ChatViewHolder;
import org.chirag.mailbot.com.adapters.viewholders.DateViewHolder;
import org.chirag.mailbot.com.adapters.viewholders.MessageViewHolder;
import org.chirag.mailbot.com.adapters.viewholders.TypingDotsHolder;
import org.chirag.mailbot.com.adapters.viewholders.ZeroHeightHolder;
import org.chirag.mailbot.com.model.ChatMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import pl.tajchert.sample.DotsTextView;

public class ChatFeedRecyclerAdapter extends SelectableAdapter implements MessageViewHolder.ClickListener {

    public static final int USER_MESSAGE = 0;
    public static final int BOT_MESSAGE = 1;
    private static final int DOTS = 8;
    private static final int NULL_HOLDER = 9;
    private static final int DATE_VIEW = 12;
    private final RequestManager glide;
    public int highlightMessagePosition = -1;
    public String query = "";
    private Context currContext;
    private Realm realm;
    private int lastMsgCount;
    private String TAG = ChatFeedRecyclerAdapter.class.getSimpleName();
    private RecyclerView recyclerView;
    private MessageViewHolder.ClickListener clickListener;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();
    private ActionMode actionMode;
    private SparseBooleanArray selectedItems;
    private AppCompatActivity currActivity;
    private Toast toast;

    private TypingDotsHolder dotsHolder;
    private ZeroHeightHolder nullHolder;
    private boolean isBotTyping = false;

    public ChatFeedRecyclerAdapter(RequestManager glide, @NonNull Context context, @Nullable OrderedRealmCollection<ChatMessage> data, boolean autoUpdate) {
        super(context, data, autoUpdate);
        this.glide = glide;
        this.clickListener = this;
        currContext = context;
        currActivity = (AppCompatActivity) context;
        lastMsgCount = getItemCount();
        selectedItems = new SparseBooleanArray();
        RealmChangeListener<RealmResults> listener = new RealmChangeListener<RealmResults>() {
            @Override
            public void onChange(RealmResults elements) {
                //only scroll if new is added.
                if (lastMsgCount < getItemCount()) {
                    scrollToBottom();
                }
                lastMsgCount = getItemCount();
            }
        };
        if (data instanceof RealmResults) {
            RealmResults realmResults = (RealmResults) data;
            realmResults.addChangeListener(listener);
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_waiting_dots, null);
        dotsHolder = new TypingDotsHolder(view);
        DotsTextView dots = dotsHolder.dotsTextView;
        dots.start();
        View view1 = inflater.inflate(R.layout.item_without_height, null);
        nullHolder = new ZeroHeightHolder(view1);
    }

    public void showDots() {
        isBotTyping = true;
    }

    public void hideDots() {
        isBotTyping = false;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        realm = Realm.getDefaultInstance();

    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
        realm.close();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view;
        switch (viewType) {
            case USER_MESSAGE:
                view = inflater.inflate(R.layout.item_user_message, viewGroup, false);
                return new ChatViewHolder(view, clickListener, USER_MESSAGE);
            case BOT_MESSAGE:
                view = inflater.inflate(R.layout.item_bot_message, viewGroup, false);
                return new ChatViewHolder(view, clickListener, BOT_MESSAGE);
            case DATE_VIEW:
                view = inflater.inflate(R.layout.date_view,viewGroup, false);
                return new DateViewHolder(view);
            case DOTS:
                return dotsHolder;
            case NULL_HOLDER:
                return nullHolder;
            default:
                view = inflater.inflate(R.layout.item_user_message, viewGroup, false);
                return new ChatViewHolder(view, clickListener, USER_MESSAGE);
        }

    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage item = getItem(position);

        if (item.getId() == -404) return DOTS;
        else if (item.getId() == -405) return NULL_HOLDER;
        else if (item.isDate()) return DATE_VIEW;
        else if (item.isMine() ) return USER_MESSAGE;
        else return BOT_MESSAGE;
    }

    @Override
    public int getItemCount() {
        if (getData() != null && getData().isValid()) {
            return getData().size() + 1;
        }
        return 0;
    }

    @Nullable
    @Override
    public ChatMessage getItem(int index) {
        if (getData() != null && getData().isValid()) {
            if (index == getData().size()) {
                if (isBotTyping) {
                    return new ChatMessage(-404, "", "", false, false, "", null);
                }
                return new ChatMessage(-405, "", "", false, false, "", null);
            }
            return getData().get(index);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ChatViewHolder) {
            ChatViewHolder chatViewHolder = (ChatViewHolder) holder;
            handleItemEvents(chatViewHolder, position);
        } else if (holder instanceof DateViewHolder) {
            DateViewHolder dateViewHolder = (DateViewHolder) holder;
            handleItemEvents(dateViewHolder, position);
        }
    }

    private void handleItemEvents(DateViewHolder dateViewHolder, int position){
        dateViewHolder.textDate.setText(getData().get(position).getDate());
    }

    private void handleItemEvents(final ChatViewHolder chatViewHolder, final int position) {
        final ChatMessage model = getData().get(position);

        chatViewHolder.backgroundLayout.setBackgroundColor(ContextCompat.getColor(currContext, isSelected(position) ? R.color.translucent_blue : android.R.color.transparent));
        if (model != null) {
            try {
                switch (getItemViewType(position)) {
                    case USER_MESSAGE:
                        chatViewHolder.chatTextView.setText(model.getContent());
                        chatViewHolder.timeStamp.setText(model.getTimeStamp());
                        if(model.getIsDelivered())
                            chatViewHolder.receivedTick.setImageResource(R.drawable.check);
                        else
                            chatViewHolder.receivedTick.setImageResource(R.drawable.clock);

                        chatViewHolder.chatTextView.setTag(chatViewHolder);
                        if (highlightMessagePosition == position) {
                            String text = chatViewHolder.chatTextView.getText().toString();
                            SpannableString modify = new SpannableString(text);
                            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(modify);
                            while (matcher.find()) {
                                int startIndex = matcher.start();
                                int endIndex = matcher.end();
                                modify.setSpan(new BackgroundColorSpan(Color.parseColor("#15577d")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            chatViewHolder.chatTextView.setText(modify);

                        }
                        chatViewHolder.timeStamp.setTag(chatViewHolder);
                        chatViewHolder.receivedTick.setTag(chatViewHolder);
                        break;
                    case BOT_MESSAGE:
                        chatViewHolder.chatTextView.setText(model.getContent());
                        chatViewHolder.timeStamp.setText(model.getTimeStamp());
                        chatViewHolder.chatTextView.setTag(chatViewHolder);
                        if (highlightMessagePosition == position) {
                            String text = chatViewHolder.chatTextView.getText().toString();
                            SpannableString modify = new SpannableString(text);
                            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(modify);
                            while (matcher.find()) {
                                int startIndex = matcher.start();
                                int endIndex = matcher.end();
                                modify.setSpan(new BackgroundColorSpan(Color.parseColor("#15577d")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            chatViewHolder.chatTextView.setText(modify);

                        }
                        chatViewHolder.timeStamp.setTag(chatViewHolder);
                        break;
                    default:
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setClipboard(String text) {

        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) currContext.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);

    }

    private void deleteMessage(final int position) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                getData().deleteFromRealm(position);
            }
        });
    }

    private void removeDates(){
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {

                RealmResults<ChatMessage> AllDates = realm.where(ChatMessage.class).equalTo("isDate",true).findAll().sort("id");

                int dateIndexFirst = getData().indexOf(AllDates.get(0));

                for(int i = 1 ; i < AllDates.size() ; i++ ){
                    int dateIndexSecond = getData().indexOf(AllDates.get(i));
                    if(dateIndexSecond == dateIndexFirst + 1) {
                        getData().deleteFromRealm(dateIndexFirst);
                        dateIndexSecond--;
                    }
                    dateIndexFirst = dateIndexSecond;
                }

                if(dateIndexFirst == getData().size() - 1 && getData().size()>0 ){
                    getData().deleteFromRealm(dateIndexFirst);
                }

            }
        });

    }

    private void scrollToBottom() {
        if (getData() != null && !getData().isEmpty() && recyclerView != null) {
            recyclerView.smoothScrollToPosition(getItemCount() - 1);
        }
    }

    private void  toggleSelectedItem(int position) {

        toggleSelection(position);
        int count = getSelectedItemCount();

        Log.d(TAG, position + " " + isSelected(position));
        selectedItems.put(position, isSelected(position));

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @Override
    public void onItemClicked(int position) {
        if (actionMode != null) {
            toggleSelectedItem(position);
        }
    }

    @Override
    public boolean onItemLongClicked(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) currContext).startSupportActionMode(actionModeCallback);
        }

        toggleSelectedItem(position);

        return true;
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @SuppressWarnings("unused")
        private final String TAG = ChatFeedRecyclerAdapter.ActionModeCallback.class.getSimpleName();
        private int statusBarColor;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection_mode, menu);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusBarColor = currActivity.getWindow().getStatusBarColor();
                currActivity.getWindow().setStatusBarColor(ContextCompat.getColor(currContext, R.color.md_teal_500));
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int nSelected;
            switch (item.getItemId()) {
                case R.id.menu_item_delete:
                    AlertDialog.Builder d = new AlertDialog.Builder(context);

                    if (getSelectedItems().size() == 1){

                        d.setMessage("Delete message?").
                                setCancelable(false).
                                setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        for (int i = getSelectedItems().size() - 1; i >= 0; i--) {
                                            deleteMessage(getSelectedItems().get(i));
                                        }
                                        removeDates();
                                        toast = Toast.makeText(recyclerView.getContext() , R.string.message_deleted , Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                        actionMode.finish();

                                    }
                                })

                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });

                    }

                    else {

                        d.setMessage("Delete " + getSelectedItems().size() + " messages?").
                                setCancelable(false).
                                setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        for (int i = getSelectedItems().size() - 1; i >= 0; i--) {
                                            deleteMessage(getSelectedItems().get(i));
                                        }
                                        removeDates();
                                        toast = Toast.makeText(recyclerView.getContext() ,getSelectedItems().size() + " Messages deleted", Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                        actionMode.finish();

                                    }
                                })

                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });

                    }


                    AlertDialog alert = d.create();
                    alert.show();
                    Button cancel = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
                    cancel.setTextColor(Color.BLUE);
                    Button delete = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                    delete.setTextColor(Color.RED);
                    return true;


                case R.id.menu_item_copy:
                    nSelected = getSelectedItems().size();
                    if (nSelected == 1) {
                        String copyText;
                        int selected = getSelectedItems().get(0);
                        copyText = getItem(selected).getContent();
                        setClipboard(copyText);
                    } else {
                        String copyText = "";
                        for (Integer i : getSelectedItems()) {
                            ChatMessage message = getData().get(i);
                            Log.d(TAG, message.toString());
                            copyText += "[" + message.getTimeStamp() + "]";
                            copyText += " ";
                            copyText += message.isMine() ? "Me: " : "Bot: ";
                            copyText += message.getContent();
                            copyText += "\n";
                            Log.d("copyText", " " + i + " " + copyText);
                        }
                        copyText = copyText.substring(0, copyText.length() - 1);

                        setClipboard(copyText);
                    }
                    if (nSelected == 1){
                        Toast toast = Toast.makeText(recyclerView.getContext() , R.string.message_copied , Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                    else {
                        Toast toast = Toast.makeText(recyclerView.getContext(), nSelected + " " + "Messages copied" , Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                    actionMode.finish();
                    return true;


                case R.id.menu_item_share:
                    nSelected = getSelectedItems().size();
                    if (nSelected == 1) {
                        int selected = getSelectedItems().get(0);
                        shareMessage(getItem(selected).getContent());
                    } else {
                        String shareText = "";
                        for (Integer i : getSelectedItems()) {
                            ChatMessage message = getData().get(i);
                            Log.d(TAG, message.toString());
                            shareText += "[" + message.getTimeStamp() + "]";
                            shareText += " ";
                            shareText += message.isMine() ? "Me: " : "Bot: ";
                            shareText += message.getContent();
                            shareText += "\n";
                        }
                        shareText = shareText.substring(0, shareText.length() - 1);
                        shareMessage(shareText);
                    }

                    actionMode.finish();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelection();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                currActivity.getWindow().setStatusBarColor(statusBarColor);
            }
            actionMode = null;
        }

        public void shareMessage(String message) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);
            sendIntent.setType("text/plain");
            currContext.startActivity(sendIntent);
        }
    }
}