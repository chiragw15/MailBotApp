package org.chirag.mailbot.com.activities;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;

import org.chirag.mailbot.com.R;
import org.chirag.mailbot.com.adapters.recycleradapters.ChatFeedRecyclerAdapter;
import org.chirag.mailbot.com.helper.Constant;
import org.chirag.mailbot.com.helper.DateTimeHelper;
import org.chirag.mailbot.com.helper.PrefManager;
import org.chirag.mailbot.com.model.ChatMessage;
import org.chirag.mailbot.com.rest.model.Datum;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

public class MainActivity extends AppCompatActivity {
    public static String TAG = MainActivity.class.getName();
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private boolean isEnabled = true;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.rv_chat_feed)
    RecyclerView rvChatFeed;
    @BindView(R.id.et_message)
    EditText ChatMessage;
    @BindView(R.id.send_message_layout)
    LinearLayout sendMessageLayout;
    @BindView(R.id.btnSpeak)
    ImageButton btnSpeak;
    private FloatingActionButton fab_scrollToEnd;
    private List<Datum> datumList = null;
    private RealmResults<ChatMessage> chatMessageDatabaseList;
    private Boolean micCheck;
    private SearchView searchView;
    private Boolean check;
    private Menu menu;
    private int pointer;
    private RealmResults<ChatMessage> results;
    private int offset = 1;
    private ChatFeedRecyclerAdapter recyclerAdapter;
    private Realm realm;
    public static String webSearch;
    private String googlesearch_query = "";
    private String video_query = "";
    private TextToSpeech textToSpeech;
    private String[] array;
    private String timenow;
    private int reminderQuery;
    private String reminder;
    private int count = 0;
    private static final String[] id = new String[1];

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                        textToSpeech.stop();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Resume playback
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        textToSpeech.stop();
                    }
                }
            };

    private Deque<Pair<String, Long>> nonDeliveredMessages = new LinkedList<>();
    TextWatcher watch = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.toString().trim().length() > 0 || !micCheck) {
                btnSpeak.setImageResource(R.drawable.ic_send_fab);
                btnSpeak.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        check = false;
                        switch (view.getId()) {
                            case R.id.btnSpeak:
                                String chat = ChatMessage.getText().toString();
                                String chat_message = chat.trim();
                                String splits[] = chat_message.split("\n");
                                String message = "";
                                for (String split : splits)
                                    message = message.concat(split).concat(" ");
                                if (!TextUtils.isEmpty(chat_message)) {
                                    sendMessage(message, chat);
                                    ChatMessage.setText("");
                                }
                                break;
                        }
                    }
                });
            } else {
                btnSpeak.setImageResource(R.drawable.ic_mic_white_24dp);
                btnSpeak.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        check = true;
                        promptSpeechInput();
                    }
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };
    private BroadcastReceiver networkStateReceiver;

    public static Boolean checkSpeechOutputPref() {
        Boolean checks = PrefManager.getBoolean(Constant.SPEECH_OUTPUT, false);
        return checks;
    }

    public static Boolean checkSpeechAlwaysPref() {
        Boolean checked = PrefManager.getBoolean(Constant.SPEECH_ALWAYS, false);
        return checked;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        compensateTTSDelay();
    }

    private void compensateTTSDelay() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            Locale locale = textToSpeech.getLanguage();
                            textToSpeech.setLanguage(locale);
                        }
                    }
                });
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            showToast(getString(R.string.speech_not_supported));
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Handler mHandler = new Handler(Looper.getMainLooper());
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<String> result = data
                                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            sendMessage(result.get(0), result.get(0));
                        }
                    });
                }
                break;
            }
        }
    }

    private void init() {
        ButterKnife.bind(this);
        realm = Realm.getDefaultInstance();
        fab_scrollToEnd = (FloatingActionButton) findViewById(R.id.btnScrollToEnd);
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new computeThread().start();
            }
        };
        Log.d(TAG, "init");
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        nonDeliveredMessages.clear();
        RealmResults<ChatMessage> nonDelivered = realm.where(ChatMessage.class).equalTo("isDelivered", false).findAll().sort("id");
        for (ChatMessage each : nonDelivered) {
            Log.d(TAG, each.getContent());
            nonDeliveredMessages.add(new Pair(each.getContent(), each.getId()));
        }
        checkEnterKeyPref();
        setupAdapter();
        rvChatFeed.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) rvChatFeed.getLayoutManager();
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() < rvChatFeed.getAdapter().getItemCount() - 5) {
                    fab_scrollToEnd.setEnabled(true);
                    fab_scrollToEnd.setVisibility(View.VISIBLE);
                } else {
                    fab_scrollToEnd.setEnabled(false);
                    fab_scrollToEnd.setVisibility(View.GONE);
                }
            }
        });
        ChatMessage.addTextChangedListener(watch);
        ChatMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String chat = ChatMessage.getText().toString();
                    String message = chat.trim();
                    if (!TextUtils.isEmpty(message)) {
                        sendMessage(message, chat);
                        ChatMessage.setText("");
                    }
                    handled = true;
                }
                return handled;
            }
        });
    }

    private void voiceReply(final String reply) {
        if ((checkSpeechOutputPref() && check) || checkSpeechAlwaysPref()) {
            final AudioManager audiofocus = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int result = audiofocus.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status != TextToSpeech.ERROR) {
                                    Locale locale = textToSpeech.getLanguage();
                                    textToSpeech.setLanguage(locale);
                                    String spokenReply = reply;
                                    textToSpeech.speak(spokenReply, TextToSpeech.QUEUE_FLUSH, null);
                                    audiofocus.abandonAudioFocus(afChangeListener);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void checkEnterKeyPref() {
        micCheck = PrefManager.getBoolean(Constant.MIC_INPUT, true);
        if (micCheck) {
            btnSpeak.setImageResource(R.drawable.ic_mic_white_24dp);
            btnSpeak.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    check = true;
                    promptSpeechInput();
                }
            });
        } else {
            check = false;
            btnSpeak.setImageResource(R.drawable.ic_send_fab);
            btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.btnSpeak:
                            String chat = ChatMessage.getText().toString();
                            String message = chat.trim();
                            if (!TextUtils.isEmpty(message)) {
                                sendMessage(message, chat);
                                ChatMessage.setText("");
                            }
                            break;
                    }
                }
            });
        }
        Boolean isChecked = PrefManager.getBoolean(Constant.ENTER_SEND, false);
        if (isChecked) {
            ChatMessage.setImeOptions(EditorInfo.IME_ACTION_SEND);
            ChatMessage.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            ChatMessage.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            ChatMessage.setSingleLine(false);
            ChatMessage.setMaxLines(4);
            ChatMessage.setVerticalScrollBarEnabled(true);
        }
    }

    private void setupAdapter() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rvChatFeed.setLayoutManager(linearLayoutManager);
        rvChatFeed.setHasFixedSize(false);
        chatMessageDatabaseList = realm.where(ChatMessage.class).findAllSorted("id");
        recyclerAdapter = new ChatFeedRecyclerAdapter(Glide.with(this), this, chatMessageDatabaseList, true);
        rvChatFeed.setAdapter(recyclerAdapter);
        rvChatFeed.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    rvChatFeed.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int scrollTo = rvChatFeed.getAdapter().getItemCount() - 1;
                            scrollTo = scrollTo >= 0 ? scrollTo : 0;
                            rvChatFeed.scrollToPosition(scrollTo);
                        }
                    }, 10);
                }
            }
        });
    }

    private void sendMessage(String query, String actual) {
        webSearch = query;
        Number temp = realm.where(ChatMessage.class).max(getString(R.string.id));
        long id;
        if (temp == null) {
            id = 0;
        } else {
            id = (long) temp + 1;
        }

        if (id == 0) {
            updateDatabase(id, " ", DateTimeHelper.getDate(), true, false, DateTimeHelper.getCurrentTime(), null);
            id++;
        } else {
            String s = realm.where(ChatMessage.class).equalTo("id", id - 1).findFirst().getDate();
            if (!DateTimeHelper.getDate().equals(s)) {
                updateDatabase(id, "", DateTimeHelper.getDate(), true, false, DateTimeHelper.getCurrentTime(), null);
                id++;
            }
        }

        updateDatabase(id, actual, DateTimeHelper.getDate(), false, true, DateTimeHelper.getCurrentTime(), null);
        nonDeliveredMessages.add(new Pair(query, id));
        new computeThread().start();
    }

    private synchronized void computeOtherMessage() {
        final String query;
        final long id;

        if (null != nonDeliveredMessages && !nonDeliveredMessages.isEmpty()) {
            if (isNetworkConnected()) {
                recyclerAdapter.showDots();
                query = nonDeliveredMessages.getFirst().first;
                id = nonDeliveredMessages.getFirst().second;
                nonDeliveredMessages.pop();

                ConversationService service = new ConversationService("2017-04-19");
                service.setUsernameAndPassword("b053dacb-cb93-40a2-aee4-b3c2cedb751f", "UrwZSyeVKtgV");


                MessageRequest newMessage = new MessageRequest.Builder()
                        .inputText(query)
                        // Replace with the context obtained from the initial request
                        //.context(...)
                        .build();

                String workspaceId = "f125e325-585c-433c-b460-70d9dab9ec1a";

                final MessageResponse response = service
                        .message(workspaceId, newMessage)
                        .execute();

                System.out.println(response);

                if (response != null ) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            realm.executeTransactionAsync(new Realm.Transaction() {
                                @Override
                                public void execute(Realm bgRealm) {
                                    try {
                                        ChatMessage chatMessage = bgRealm.where(ChatMessage.class).equalTo("id", id).findFirst();
                                        chatMessage.setIsDelivered(true);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }});
                            rvChatFeed.getRecycledViewPool().clear();
                            recyclerAdapter.notifyItemChanged((int) id);
                            final String setMessage = response.getText().get(0);
                            addNewMessage(setMessage, datumList);
                            if(checkSpeechAlwaysPref())
                                voiceReply(setMessage);
                            recyclerAdapter.hideDots();
                        }
                    });

                } else {

                    if (!isNetworkConnected()) {
                        recyclerAdapter.hideDots();
                        nonDeliveredMessages.addFirst(new Pair(query, id));
                        Snackbar snackbar = Snackbar.make(coordinatorLayout,
                                getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }

                if (isNetworkConnected())
                    computeOtherMessage();
            }
        }
    }

    private void addNewMessage(String answer, List<Datum> datumList) {
        Number temp = realm.where(ChatMessage.class).max(getString(R.string.id));
        long id;
        if (temp == null) {
            id = 0;
        } else {
            id = (long) temp + 1;
        }
        updateDatabase(id, answer, DateTimeHelper.getDate(), false, false, DateTimeHelper.getCurrentTime(), datumList);
    }

    private void updateDatabase(final long id, final String message, final String date,
                                final boolean isDate, final boolean mine, final String timeStamp,
                                final List<Datum> datumList) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                ChatMessage chatMessage = bgRealm.createObject(ChatMessage.class, id);
                chatMessage.setContent(message);
                chatMessage.setDate(date);
                chatMessage.setIsDate(isDate);
                chatMessage.setIsMine(mine);
                chatMessage.setTimeStamp(timeStamp);
                if (mine)
                    chatMessage.setIsDelivered(false);
                else
                    chatMessage.setIsDelivered(true);
                if (datumList != null) {
                    RealmList<Datum> datumRealmList = new RealmList<>();
                    for (Datum datum : datumList) {
                        Datum realmDatum = bgRealm.createObject(Datum.class);
                        realmDatum.setDescription(datum.getDescription());
                        realmDatum.setLink(datum.getLink());
                        realmDatum.setTitle(datum.getTitle());
                        datumRealmList.add(realmDatum);
                    }
                    chatMessage.setDatumRealmList(datumRealmList);
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Log.v(TAG, getString(R.string.updated_successfully));
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);

        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage.setVisibility(View.GONE);
                btnSpeak.setVisibility(View.GONE);
                sendMessageLayout.setVisibility(View.GONE);
                isEnabled = false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                modifyMenu(false);
                recyclerAdapter.highlightMessagePosition = -1;
                recyclerAdapter.notifyDataSetChanged();
                searchView.onActionViewCollapsed();
                offset = 1;
                ChatMessage.setVisibility(View.VISIBLE);
                btnSpeak.setVisibility(View.VISIBLE);
                sendMessageLayout.setVisibility(View.VISIBLE);
                return false;
            }

        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //// Handle Search Query
                ChatMessage.setVisibility(View.GONE);
                btnSpeak.setVisibility(View.GONE);
                sendMessageLayout.setVisibility(View.GONE);
                results = realm.where(ChatMessage.class).contains(getString(R.string.content),
                        query, Case.INSENSITIVE).findAll();
                recyclerAdapter.query = query;
                offset = 1;
                Log.d(TAG, String.valueOf(results.size()));
                if (results.size() > 0) {
                    modifyMenu(true);
                    pointer = (int) results.get(results.size() - offset).getId();
                    Log.d(TAG,
                            results.get(results.size() - offset).getContent() + "  " +
                                    results.get(results.size() - offset).getId());
                    searchMovement(pointer);
                } else {
                    showToast(getString(R.string.not_found));
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    modifyMenu(false);
                    recyclerAdapter.highlightMessagePosition = -1;
                    recyclerAdapter.notifyDataSetChanged();
                } else {
                    ChatMessage.setVisibility(View.GONE);
                    btnSpeak.setVisibility(View.GONE);
                    sendMessageLayout.setVisibility(View.GONE);
                    results = realm.where(ChatMessage.class).contains(getString(R.string.content),
                            newText, Case.INSENSITIVE).findAll();
                    if(results.size() == 0){
                        return false;
                    }
                    recyclerAdapter.query = newText;
                    offset = 1;
                    Log.d(TAG, String.valueOf(results.size()));
                    modifyMenu(true);
                    pointer = (int) results.get(results.size() - offset).getId();
                    Log.d(TAG,
                            results.get(results.size() - offset).getContent() + "  " +
                                    results.get(results.size() - offset).getId());
                    searchMovement(pointer);
                }
                return false;
            }
        });
        return true;
    }

    private void searchMovement(int position) {
        rvChatFeed.scrollToPosition(position);
        recyclerAdapter.highlightMessagePosition = position;
        recyclerAdapter.notifyDataSetChanged();
    }

    private void modifyMenu(boolean show) {
        menu.findItem(R.id.up_angle).setVisible(show);
        menu.findItem(R.id.down_angle).setVisible(show);
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {

            if (!isEnabled) {
                ChatMessage.setVisibility(View.VISIBLE);
                btnSpeak.setVisibility(View.VISIBLE);
                sendMessageLayout.setVisibility(View.VISIBLE);
            }
            modifyMenu(false);
            recyclerAdapter.highlightMessagePosition = -1;
            recyclerAdapter.notifyDataSetChanged();
            searchView.onActionViewCollapsed();
            offset = 1;
            return;
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.up_angle:
                offset++;
                if (results.size() - offset > -1) {
                    pointer = (int) results.get(results.size() - offset).getId();
                    Log.d(TAG, results.get(results.size() - offset).getContent() + "  " +
                            results.get(results.size() - offset).getId());
                    searchMovement(pointer);
                } else {
                    showToast(getString(R.string.nothing_up_matches_your_query));
                    offset--;
                }
                break;
            case R.id.down_angle:
                offset--;
                if (results.size() - offset < results.size()) {
                    pointer = (int) results.get(results.size() - offset).getId();
                    Log.d(TAG, results.get(results.size() - offset).getContent() + "  " +
                            results.get(results.size() - offset).getId());
                    searchMovement(pointer);
                } else {
                    showToast(getString(R.string.nothing_down_matches_your_query));
                    offset++;
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nonDeliveredMessages.clear();
        RealmResults<ChatMessage> nonDelivered = realm.where(ChatMessage.class).equalTo("isDelivered", false).findAll().sort("id");
        for (ChatMessage each : nonDelivered)
            nonDeliveredMessages.add(new Pair(each.getContent(), each.getId()));
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        checkEnterKeyPref();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(networkStateReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void scrollToEnd(View view) {
        rvChatFeed.smoothScrollToPosition(rvChatFeed.getAdapter().getItemCount() - 1);
    }

    private class computeThread extends Thread {
        public void run() {
            computeOtherMessage();
        }
    }
}
