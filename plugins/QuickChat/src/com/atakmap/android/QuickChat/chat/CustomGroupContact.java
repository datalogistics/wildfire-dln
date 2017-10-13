
package com.atakmap.android.QuickChat.chat;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.GroupContact;

import java.util.List;

/**
 * Created by Scott Auman on 11/7/2016.
 * a extension group of group contact that allows for the user
 * of the boolean variable enter-able which dictates if a group has the
 * ability to jump into the next level of groups
 */

public class CustomGroupContact extends GroupContact {

    private boolean enterable; //does the group contain more groups?

    /**
     * @param uid the hex string for a user
     * @param callsign the string version
     * @param contacts collection of contact objects
     * @param userCreated was this user created group?
     * @param enterable does this group have more group contacts inside?
     */
    public CustomGroupContact(String uid, String callsign,
            List<Contact> contacts, boolean userCreated, boolean enterable) {
        super(uid, callsign, contacts, userCreated);
        setEnterable(enterable);
    }

    /**
     * @param uid the hex string for a user
     * @param callsign the string version
     * @param userCreated was this user created group?
     * @param enterable does this group have more group contacts inside?
     */
    public CustomGroupContact(String uid, String callsign, boolean userCreated,
            boolean enterable) {
        super(uid, callsign, userCreated);
        setEnterable(enterable);
    }

    /**
     * @param uid the hex string for a user
     * @param callsign the string version
     * @param enterable does this group have more group contacts inside?
     */
    public CustomGroupContact(String uid, String callsign, boolean enterable) {
        super(uid, callsign);
        setEnterable(enterable);
    }

    public boolean isEnterable() {
        return enterable;
    }

    private void setEnterable(boolean enterable) {
        this.enterable = enterable;
    }
}
