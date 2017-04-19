package org.chirag.mailbot.com.model;

import org.chirag.mailbot.com.rest.model.Datum;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatMessage extends RealmObject {

    @PrimaryKey
    private long id;
    private String content, timeStamp, date;
    private RealmList<Datum> datumRealmList;
    private boolean isMine, isDelivered, isDate;

    public ChatMessage() {
        datumRealmList = new RealmList<>();
    }

    public ChatMessage(long id, String content, String date, boolean isDate, boolean isMine, String timeStamp, RealmList<Datum> datumRealmList) {
        this.id = id;
        this.isMine = isMine;
        this.content = content;
        this.date = date;
        this.isDate = isDate;
        this.timeStamp = timeStamp;
        this.datumRealmList = datumRealmList;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDate() {
        return isDate;
    }

    public void setIsDate(boolean date) {
        isDate = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setIsMine(boolean isMine) {
        this.isMine = isMine;
    }

    public RealmList<Datum> getDatumRealmList() { return datumRealmList; }

    public void setDatumRealmList(RealmList<Datum> datumRealmList) {
        this.datumRealmList = datumRealmList;
    }

    public boolean getIsDelivered() { return isDelivered; }

    public void setIsDelivered(boolean isDelivered) { this.isDelivered = isDelivered; }

}
