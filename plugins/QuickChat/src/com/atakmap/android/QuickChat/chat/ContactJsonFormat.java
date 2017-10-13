
package com.atakmap.android.QuickChat.chat;

import android.content.Context;

import com.atakmap.android.maps.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Scott Auman on 4/22/2016.
 */
class ContactJsonFormat {

    private final static String CALLSIGN = "callsign";
    private final static String UID = "uid";
    private final static String ARRAY_NAME = "contacts";
    private JSONArray mainArray;

    public ContactJsonFormat(String string) {
        JSONObject mainObject;
        try {
            mainObject = new JSONObject(string);
            mainArray = mainObject.getJSONArray(ARRAY_NAME);
        } catch (JSONException e) {
            e.printStackTrace();
            mainArray = new JSONArray();
        }
    }

    public ContactJsonFormat() {
        //empty constructor
    }

    public String convertObjectsToJson(List<PopUpUser> users)
            throws JSONException {

        JSONArray array = new JSONArray();
        for (PopUpUser popUpUser : users) {
            JSONObject obj = new JSONObject();
            obj.put(CALLSIGN, popUpUser.getName());
            obj.put(UID, popUpUser.getUid());
            array.put(obj);
        }
        return new JSONObject().put(ARRAY_NAME, array).toString();
    }

    /**
     * @param uuid   the uuid of the target chat user
     * @param string the entire json string containing all the users
     * @return true/false if user is contained in list
     * @throws JSONException
     */
    public boolean findSpecificUser(String uuid, String string)
            throws JSONException {
        this.mainArray = new JSONObject(string).getJSONArray(ARRAY_NAME);
        for (int i = 0; i < mainArray.length(); i++) {
            JSONObject obj = mainArray.getJSONObject(i);
            if (obj.getString(UID).equals(uuid))
                return true;
        }
        return false;
    }

    public String removeSpecificUser(PopUpUser popUpUser) throws JSONException {
        List<PopUpUser> list = new ArrayList<PopUpUser>();
        List<PopUpUser> saved = getAllSavedContacts();
        Iterator<PopUpUser> iter = saved.iterator();
        while (iter.hasNext()) {
            PopUpUser popUser = iter.next();
            if (popUser.getUid().equals(popUpUser.getUid())) {
                iter.remove();
            } else {
                list.add(popUser);
            }
        }
        return convertObjectsToJson(list);
    }

    public void updateUserCallsign(Context context, PopUpUser popUpUser)
            throws JSONException {

        List<PopUpUser> list = new ArrayList<PopUpUser>();
        Iterator<PopUpUser> iter = SavedFilteredPopupChatUsers
                .getEntireUserListPopUpUsers(context).iterator();
        while (iter.hasNext()) {
            PopUpUser popUser = iter.next();
            if (popUser.getUid().equals(popUpUser.getUid())) {
                popUser = popUpUser;
                list.add(popUser);
            } else {
                list.add(popUser);
            }
        }
        SavedFilteredPopupChatUsers.saveNewUserList(MapView.getMapView()
                .getContext(), convertObjectsToJson(list));
    }

    public String addAndConvert(PopUpUser popUpUser) throws JSONException {
        List<PopUpUser> list = getAllSavedContacts();
        list.add(popUpUser);
        return convertObjectsToJson(list);
    }

    /**
     * @return listing of all filtered users wrapped in
     * a POpUpUser object
     */
    public List<PopUpUser> getAllSavedContacts() {

        List<PopUpUser> list = new ArrayList<PopUpUser>();

        String name;
        String uid;
        for (int i = 0; i < mainArray.length(); i++) {
            final JSONObject obj;
            try {
                obj = mainArray.getJSONObject(i);
                name = obj.getString(CALLSIGN);
                uid = obj.getString(UID);
                //make a contact object and store at toString()
                PopUpUser popUpUser = new PopUpUser(name, uid);
                list.add(popUpUser);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
