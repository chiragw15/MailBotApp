
package org.chirag.mailbot.com.rest.model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class Metadata {

    @SerializedName("count")
    @Expose
    Integer count;

    public Integer getCount() {
        return count;
    }
}
