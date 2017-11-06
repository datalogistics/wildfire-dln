package com.atakmap.android.takchat.contact;

import android.content.SharedPreferences;

import com.atakmap.android.contact.Connector;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.contact.XmppConnector;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.coremap.log.Log;

/**
 * Represents an ATAK Contact list contact, for an XMPP buddy
 * Currently displayed only if available (online) and does not have a TAK User UID mapping b/c
 * in the latter case, the contact is already in the contact list
 *
 * Created by byoung on 12/20/16.
 */
public class XmppIndividualContact extends IndividualContact {

    private static final String TAG = "XmppIndividualContact";
    private static final String UID_PREFIX = "xmpp.";

    private final XmppContact _contact;
    private boolean _bInContactList;

    public XmppIndividualContact(XmppContact contact) {
        super(contact.getName(), UID_PREFIX + contact.getId().toString());
        _contact = contact;
        _bInContactList = false;

        setDispatch(false);
        addConnector(new XmppConnector(contact.getId().toString()));
        setDispatch(true);

        if(TAKChatUtils.isMe(contact.getId()))
            setDispatch(false);
        setUpdateStatus(UpdateStatus.DEAD);
    }

    public synchronized boolean isInContactList(){
        return _bInContactList;
    }

    public synchronized void setInContactList(boolean bInContactList) {
        this._bInContactList = bInContactList;
    }

    @Override
    public String getIconUri() {
        if(TAKChatUtils.isConference(_contact))
            return "android.resource://"
                    + MapView.getMapView().getContext().getPackageName()
                    + "/" + com.atakmap.app.R.drawable.group_icon;
        else
            return "android.resource://"
                + MapView.getMapView().getContext().getPackageName()
                + "/" + com.atakmap.app.R.drawable.xmpp_icon;
    }

    @Override
    public Connector getDefaultConnector(SharedPreferences prefs){
        return getConnector(XmppConnector.CONNECTOR_TYPE);
    }

    /**
     * Add or Remove contact to contact list if available, and not already there as a TAK user
     * If in contact list set status to current or stale
     *
     */
    public void setUpdateStatus() {
        Log.d(TAG, "setUpdateStatus: " + getName() + ", " + _contact.isAvailable() + ", " + _contact.isAway());

        //if this is "me" or contact is mapped to a TAK user, and user is in contact, remove it
        if(TAKChatUtils.isMe(_contact.getId()) ||
                (_contact.hasTakUserUID() && Contacts.getInstance().getContactByUuid(
                _contact.getTakUserUID()) != null)){
            //do not add this contact if already there as a TAK user
            if(isInContactList()) {
                Log.d(TAG, "removing mapped contact: " + this.toString());
                Contacts.getInstance().removeContact(this);
                setInContactList(false);
            }

            //refresh ATAK contact list for TAK mapped contacts
            dispatchChangeEvent();
            return;
        }

        //see if available
        if(_contact.isAvailable()){
            if(!isInContactList()) {
                Log.d(TAG, "Adding contact list ref: " + _contact.toString());
                Contacts.getInstance().addContact(this);
                setInContactList(true);
            }
            if (_contact.isAway()) {
                super.stale();
            } else {
                super.current();
            }
        }else{
            //super.die();
            if(isInContactList()) {
                Log.d(TAG, "Removing contact list ref: " + _contact.toString());
                Contacts.getInstance().removeContact(this);
                setInContactList(false);
            }
        }
    }

    public void removeListRef() {
        if(isInContactList()) {
            Log.d(TAG, "Removing contact list ref: " + _contact.toString());
            Contacts.getInstance().removeContact(this);
            setInContactList(false);
        }
    }
}
