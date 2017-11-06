
package com.atakmap.android.QuickChat.chat;

/**
 * Created by AumanS on 4/22/2016.
 * A cloned Contact object that stores the name, and uid for a ATAK user
 * checked is set when displaying in a listview/adapter
 */
public class PopUpUser {

    public static final String DEFAULT_NAME = "Unknown";

    public void setName(String name) {
        this.name = name;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    private String name;
    private String uid;
    private boolean checked;

    public PopUpUser(String name, String uid) {
        setName(name);
        setUid(uid);
    }

    public PopUpUser() {

    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
