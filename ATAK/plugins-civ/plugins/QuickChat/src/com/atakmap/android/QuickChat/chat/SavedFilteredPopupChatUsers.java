package com.atakmap.android.QuickChat.chat;

import android.content.Context;
import android.preference.PreferenceManager;

import com.atakmap.android.maps.MapView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AumanS on 4/6/2016.
 * handles the saved json string and objects storing
 * the users filtered for message popups
 */
public class SavedFilteredPopupChatUsers {

    private static final String SAVEDFILTEREDPOPUPCHATUSERS = "saved_filteredpopupchatusers";

    /*
        JSON FORMAT STORED IN MEMORY EXAMPLE
        {
                {"contacts":[{"callsign":"XXXX_dev#s5","uid":"ANDROID-XX:C2:XX:4F:77:XX"},
                {"callsign":"XXXXX","uid":"ANDROID-C0:XX:XX:XX:4A:XX"}]}
        }
     */

    /**
     * @param string  the json string containing all user enteries as json object s
     */
    public static void saveNewUserList(Context context, String string) {
        System.out.println("SAVED THIS  LIST " + string);
        PreferenceManager.getDefaultSharedPreferences(MapView.getMapView()
                .getContext()).edit()
                .putString(SAVEDFILTEREDPOPUPCHATUSERS, string).apply();
    }

    /**
     * @return @String containing the full json of every user in JSON Object notation
     */
    private static String getEntireUserList(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext()).getString(
                SAVEDFILTEREDPOPUPCHATUSERS, getDefault());
    }

    /**Returns the entire list of users in A LIST of PopupUsers
     * this returns the list with each user object created already
       only returns the full string un edited
     */
    static List<PopUpUser> getEntireUserListPopUpUsers(Context context) {
        return new ContactJsonFormat(getEntireUserList(context))
                .getAllSavedContacts();
    }

    /**Searches the json saved string in memory for a specific user
     * to remove from the list
     * @param user PopUpUser object
     * @return String that contains the json object removed
     */
    static String removeUserFromList(Context context, PopUpUser user) {

        ContactJsonFormat contactJsonFormat = new ContactJsonFormat(
                getEntireUserList(context));
        try {
            return contactJsonFormat.removeSpecificUser(user);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param popUpUser add the object to the current list of all users
     * @return a string json format of all users list
     */
    public static String addSingleUser(Context context, PopUpUser popUpUser) {
        try {
            //make sure list is active and not "" we can not create a json obj from the "" empty string
            if (getEntireUserList(context).equals("")) {
                SavedFilteredPopupChatUsers.saveNewUserList(MapView
                        .getMapView().getContext(), getDefault());
            }

            ContactJsonFormat contactJsonFormat = new ContactJsonFormat(
                    getEntireUserList(context));
            return contactJsonFormat.addAndConvert(popUpUser);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void changeCallsignForUser(Context context,
            PopUpUser popUpUser) {
        ContactJsonFormat contactJsonFormat = new ContactJsonFormat();
        try {
            contactJsonFormat.updateUserCallsign(context, popUpUser);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**Returns a string that is used as the default json
     * @return @String
     */
    public static String getDefault() {
        return "{ " +
                "\"contacts\"" +
                ":[" +
                "]" +
                " }";
    }

    /**Checks each PopUpUser object's uid
     * returns true if user was found in the current saved list
     * false if the user was not in the list
     * @param targetUID the uid @String we are searching for
     * @return boolean
     */
    public static boolean isUserInList(Context context, String targetUID) {
        ContactJsonFormat contactJsonFormat = new ContactJsonFormat(
                getEntireUserList(context));
        try {
            return contactJsonFormat.findSpecificUser(targetUID,
                    getEntireUserList(context));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * @param s       the string of chars we are searching for
     * @return a List<PopUpUser> of users who have matching chars in their callsigns</>
     * The method searches and matched based on every char in the string
     */
    public static List<PopUpUser> searchForUser(Context context, String s) {

        s = s.toLowerCase(); //lowercase the incoming search string

        List<PopUpUser> users = new ArrayList<PopUpUser>();
        ContactJsonFormat contactJsonFormat = new ContactJsonFormat(
                getEntireUserList(context));
        List<PopUpUser> list = contactJsonFormat.getAllSavedContacts();

        //if no entry then return entire list!
        if (s.length() == 0) {
            return list;
        }
        //loop through each PopUpUser object extracting the callsign name
        //String.compare(string) to check each char in order
        for (PopUpUser popper : list) {
            if (popper.getName() != null || popper.getName().length() == 0) {
                String name = popper.getName().toLowerCase();
                if (name.contains(s)) {
                    users.add(popper);
                }
            }
        }
        return users; //return list with users matching the search string back to calling class
    }
}
