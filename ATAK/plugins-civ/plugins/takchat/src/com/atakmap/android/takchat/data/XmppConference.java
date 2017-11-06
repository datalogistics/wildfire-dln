package com.atakmap.android.takchat.data;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.jid.BareJid;

/**
 * Represents a conference aka "group chat"
 *
 * Created by byoung on 8/11/2016.
 */
public class XmppConference extends XmppContact {

    private static final String TAG = "XmppConference";

    private MultiUserChat _mucchat;
    private String password;

    public XmppConference(String name, BareJid jid, String passwd){
        super(name, jid);
        this.password = passwd;
    }

    public XmppConference(MultiUserChat mucchat, String name) {
        super(name, mucchat.getRoom());
        Log.i(TAG, "Creating conference: " + this.getName() + ": " + this.getId());
        _mucchat = mucchat;
    }

    public MultiUserChat getMUC() {
        return _mucchat;
    }

    public void setMUC(MultiUserChat chat){
        if(chat == null){
            Log.w(TAG, "Set invalid MUC");
            return;
        }

        Log.d(TAG, "Set: " + chat.toString());
        _mucchat = chat;

        if(FileSystemUtils.isEmpty(this.name) && !FileSystemUtils.isEmpty(_mucchat.getSubject()))
            this.name = _mucchat.getSubject();

        this.id = chat.getRoom();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object conf) {
        if(XmppConference.class.isInstance(conf)) {
            return ((XmppConference)conf).getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public String toString() {
        //TODO error checking?
        //TODO dont print password length after testing
        return String.format("conference: %s, %s, %d", id.toString(), name, (FileSystemUtils.isEmpty(password) ? 0 : password.length()));
    }
}
