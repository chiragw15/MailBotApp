package org.chirag.mailbot.com.helper;

import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeHelper {

    public static String getCurrentTime() {
        String delegate = "hh:mm aaa";
        return (String) DateFormat.format(delegate,Calendar.getInstance().getTime());
    }

    public static String getDate(){
        SimpleDateFormat sdf = new SimpleDateFormat(" MMM dd yyyy");
        return sdf.format(new Date());
    }
}
