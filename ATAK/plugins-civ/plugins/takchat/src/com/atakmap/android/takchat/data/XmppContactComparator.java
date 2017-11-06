package com.atakmap.android.takchat.data;

import com.atakmap.android.takchat.TAKChatUtils;

import java.util.Comparator;

/**
 * Compares contacts for sorting
 * See also <code>{@link com.atakmap.android.takchat.adapter.ContactListAdapter.contactComparator}</code>
 *
 * Created by byoung on 10/7/16.
 */
public class XmppContactComparator implements Comparator<XmppContact> {


    @Override
    public int compare(XmppContact lhs, XmppContact rhs) {
        if(lhs == null)
            return 1;
        else if(rhs == null)
            return -1;

        //first sort by status
        if(lhs.isAvailable() != rhs.isAvailable()){
            return lhs.isAvailable() ? -1 : 1;
        }
        if(lhs.isAway() != rhs.isAway()){
            return lhs.isAway() ? 1 : -1;
        }

        //then by name (if both are contacts or conferences)
        if(TAKChatUtils.isConference(lhs) == TAKChatUtils.isConference(rhs)){
            return lhs.getName().compareTo(rhs.getName());
        }

        //finally put confs above contacts
        return TAKChatUtils.isConference(lhs) ? -1 : 1;
    }
}
