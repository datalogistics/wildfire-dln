
package com.atakmap.android.QuickChat.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.QuickChat.plugin.QuickChatLifecycle;
import com.atakmap.android.QuickChat.plugin.R;
import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.chat.TeamGroup;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.GroupContact;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;

import java.util.List;

/**
 * Created by Scott Auman (Par Governement) on 4/20/2016.
 * Handles the dropdown UI for displaying the
 * options and adapter that shows all users currently on filtered list to receive a popup message
 * each option is given a "state"  , when states (enumeration) changes visibility on certain UI elements and icons
 * The top options bar similar to the contactDropdown, the bar contains a scrollview just in case icons fill out the entire screen
 * there are 6 icons total that can be displayed at one time.
 */
public class FilterChatUserDropDown extends DropDownReceiver implements
        DropDown.OnStateListener, AddUsersAdapter.ListTypes {

    private static final String TAG = "FilterChatUserDropDown";

    private final Context context;
    private ListView filterListView;
    private TextView titleTextView, noSavedUsersTextView,
            noUsersConnectedTextView;
    private ImageButton add, delete, search, sort, backImageButton, ok,
            deleteAll;
    private ChatPopFilterAdapter chatPopFilterAdapter;
    private EditText searchEditText;
    private LinearLayout listingLayout, addUsersLayout;
    private RelativeLayout containerLayout;
    private Button addUsers, addGroup;
    private ListView addUsersListView;
    private SingleAdapter contactListAdapter;
    private boolean searchStatus, adding;
    private View root;
    private UserGroupsAdapter userGroupsAdapter;
    private TeamsAdapter teamsAdapter;

    private final String[] tips = {
            "Add A New User To The List",
            "Delete User(s) From This List", "Sort Users Callsigns A-Z",
            "Search For User(s) By Callsign", "Return Back To Chat Menu",
            "Messages From Users In This List Will Show as Banner Popups",
            "Go Straight To Plugin Preferences"
    };
    private AddUsersAdapter addUsersAdapter;

    @Override
    public void onGroupChange(AddUsersAdapter.TYPES type, List<?> list) {
        switch (type) {
            case MAIN:
                if (addUsersAdapter != null) {
                    addUsersListView.setAdapter(addUsersAdapter);
                }
                break;
            case CHILD:
                //not used yet
                break;
            case USER_GROUPS:
                if (list != null) {
                    if (list.size() == 0) {
                        Toast.makeText(
                                MapView.getMapView().getContext(),
                                PluginHelper
                                        .getPluginStringFromResources(R.string.no_users_groups_error_message_text),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        setupUserGroupsAdapter((List<GroupContact>) list);
                    }
                }
                break;
            case TEAMS:
                if (list != null) {
                    //noinspection unchecked
                    setupTeamsAdapter((List<TeamGroup>) list);
                }
                break;
            default:
                break;
        }
    }

    private void setupUserGroupsAdapter(List<GroupContact> list) {
        userGroupsAdapter = new UserGroupsAdapter(
                context, list);
        addUsersListView.setAdapter(userGroupsAdapter);
        addUsersListView.setOnItemLongClickListener(userGroupsAdapter);
    }

    private void setupTeamsAdapter(List<TeamGroup> list) {
        teamsAdapter = new TeamsAdapter
                (context, list);
        addUsersListView.setAdapter(teamsAdapter);
        addUsersListView.setOnItemLongClickListener(teamsAdapter);
        PreferenceManager.getDefaultSharedPreferences(
                MapView.getMapView().getContext())
                .registerOnSharedPreferenceChangeListener(teamsAdapter);
    }

    /**
     * MODE STATES
     * changes visibility of specific icons on menu
     */
    public enum MODES {
        ADD, DELETE, SEARCH, SORT, MAIN, BACK
    }

    /**Constructor
     * sets the contexts used , because its a plugin and UI on the original map.
     */
    public FilterChatUserDropDown(MapView mapView, Context pluginContext) {
        super(mapView);
        this.context = pluginContext;

        LayoutInflater inf = (LayoutInflater) pluginContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inf.inflate(R.layout.filter_chat_user_dropdown, null);
        setRoot(root); //attach main view
        createJavaElements(root); //build java variables
        setClickers(); //simple onClickListeners
        setLongClickers(); //long presses clickers
        /*
        handles the search edittext interaction with the keyboard
        when a user presses a key the text watcher calls onTextChanged()
        we take the string the user typed in and call search() in SavedPopupChatUsers
        the method searches every current callsign and returns a list matching the regex the user typed in
        */
        TextWatcher searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                //removing the no search items
                if (listingLayout.getChildCount() > 1) {
                    listingLayout.removeViewAt(0);
                }
                if (count == 0) {
                    chatPopFilterAdapter.refresh(); //defaults the list
                } else {
                    chatPopFilterAdapter.setNewData(SavedFilteredPopupChatUsers
                            .searchForUser
                            (context, s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        searchEditText.addTextChangedListener(searchWatcher);
    }

    private void addUsersToContactList() {
        int added = 0;
        int alreadyAdded = 0;

        BaseAdapter adapter = (BaseAdapter) addUsersListView.getAdapter();

        //determine which adapter is/was attached and grab the clicked users from there
        if (adapter instanceof SingleAdapter) {
            if (contactListAdapter != null
                    && contactListAdapter.getSelected().size() > 0) {
                for (String string : contactListAdapter.getSelected()) {
                    //save each uid into prefs list
                    if (addContactUIDToList(string)) {
                        added++;
                    } else {
                        alreadyAdded++;
                    }
                }
                //group impl
            }
        } else if (adapter instanceof AddUsersAdapter) {
            //nothing yet!
        } else if (adapter instanceof TeamsAdapter) {
            for (String s : ((TeamsAdapter) adapter).getClickedGroups()) {
                if (addContactUIDToList(s)) {
                    added++;
                } else {
                    alreadyAdded++;
                }
            }
        } else if (adapter instanceof UserGroupsAdapter) {
            for (String s : ((UserGroupsAdapter) adapter).getCheckedBoxes()) {
                if (addContactUIDToList(s)) {
                    added++;
                } else {
                    alreadyAdded++;
                }
            }
        }
        disbatchAddedUsersMessage(added, alreadyAdded);
        setIconMode(MODES.MAIN);
    }

    /**
     * set widget click listeners to be invoked when event fires a click event
     */
    private void setClickers() {
        addUsers.setOnClickListener(onClickListener);
        addGroup.setOnClickListener(onClickListener);
        deleteAll.setOnClickListener(onClickListener);
        backImageButton.setOnClickListener(onClickListener);
        delete.setOnClickListener(onClickListener);
        ok.setOnClickListener(onClickListener);
        sort.setOnClickListener(onClickListener);
        add.setOnClickListener(onClickListener);
        search.setOnClickListener(onClickListener);
    }

    /**
     * handles the click listeners for the buttons we added this javabean too
     */
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.individualButton:
                    setIndividualList();
                    break;
                case R.id.groupsButton:
                    setGroupList();
                    break;
                case R.id.deleteAllButton:
                    deleteAllContacts();
                    break;
                case R.id.deleteFilterUserImageButton:
                    setIconMode(MODES.DELETE);
                    break;
                case R.id.okButton:
                    addUsersToContactList();
                    break;
                case R.id.sortImageButton:
                    setIconMode(MODES.SORT);
                    break;
                case R.id.addFilterUserImageButton:
                    setIconMode(MODES.ADD);
                    break;
                case R.id.searchFilterImageButton:
                    setIconMode(MODES.SEARCH);
                    break;
                case R.id.backImageButton:
                    setIconMode(MODES.BACK);
                    break;
            }
        }
    };

    /**adds the user to the current viewable list(ie filterList)
     * @param string uid of contact
     * @return true if added false otherwise
     */
    private boolean addContactUIDToList(String string) {

        //weird behavior selecting indiv contacts and get group UID's...
        //bandaid fix for now...-SA
        if (string.equals("All Chat Rooms") || string.equals("All Streaming")) {
            return false;
        }
        if (!SavedFilteredPopupChatUsers.isUserInList(context, string)) {
            PopUpUser popper = new PopUpUser();
            popper.setUid(string);
            popper.setName(PluginHelper.getCallsignFromContactUid(string));
            SavedFilteredPopupChatUsers.saveNewUserList(MapView.getMapView()
                    .getContext(),
                    SavedFilteredPopupChatUsers.addSingleUser(context, popper));
            return true;
        } else {
            Log.d(TAG, "Not a valid contact");
        }
        return false;
    }

    /**
     * handles when items in an adapter are in the checked state = true
     * need to handle this so we can hide/show certain icons
     */
    public interface ItemsChecked {
        void itemsChecked(boolean checked);

        void noItemsInSearch();
    }

    public void onDestroy(){
        Log.d(QuickChatLifecycle.TAG,"Destroying Filter Drop Down");
        if(isVisible()){
            closeDropDown();
        }
    }

    @Override
    protected void disposeImpl() {

    }

    /**
     * Change visibility and variables based on the mode sent in
     * used as a code cleanup and easier to interact with all fo the different icons and views
     *
     * @param mode the enum mode we are switching too
     */
    private void setIconMode(MODES mode) {
        switch (mode) {
            case SEARCH:
                searchUsers();
                break;
            case MAIN:
                setUpMainUI();
                break;
            case SORT:
                sortUsers();
                break;
            case ADD:
                addUsers();
                break;
            case DELETE:
                deleteUsers();
                break;
            case BACK:
                if(!handleBackState()){
                    closeDropDown();
                }
                break;
        }
    }

    @Override
    protected boolean onBackButtonPressed() {
       return handleBackState();
    }

    private boolean handleBackState() {

        if (addUsersListView.getAdapter() instanceof AddUsersAdapter) {
            setIconMode(MODES.MAIN);

        } else if (addUsersListView.getAdapter() instanceof TeamsAdapter) {
            setGroupList();

        } else if (addUsersListView.getAdapter() instanceof UserGroupsAdapter) {
            setGroupList();
            /*
                close dropdown if not in search mode
             */
        } else if (!searchStatus && !adding) {
            return false;
        } else {
            //reset back to default main
            if (searchStatus)
                searchStatus = false;

            if (adding)
                adding = false;

            if (listingLayout.getChildCount() > 1)
                listingLayout.removeViewAt(0);
            setIconMode(MODES.MAIN);
        }
        hideKeyboard(); //JIC close keyboard if still showing
        return true;
    }

    private void deleteUsers() {
        if (chatPopFilterAdapter.getCheckedBoxes().size() == 0) {
            Toast.makeText(MapView.getMapView().getContext(),
                    "No User(s) Selected", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(MapView.getMapView().getContext())
                    .setTitle(
                            "Delete Selected "
                                    + ((chatPopFilterAdapter.getCheckedBoxes()
                                            .size() > 1) ? "Users" : "User"))
                    .setMessage(
                            "Remove Selected "
                                    + (chatPopFilterAdapter.getCheckedBoxes()
                                            .size() > 1 ? "Users" : "User")
                                    + " From Receiving Popup Messages From?")
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // continue with delete
                                    PopUpUser popUp;
                                    for (ChatPopFilterAdapter.UsersViewHolder holders : chatPopFilterAdapter
                                            .getCheckedBoxes()) {
                                        //delete each user from the saved set<string> listing
                                        popUp = new PopUpUser();
                                        popUp.setUid(holders.uuid);
                                        popUp.setName(holders.name.getText()
                                                .toString());
                                        SavedFilteredPopupChatUsers
                                                .saveNewUserList(
                                                        MapView.getMapView()
                                                                .getContext(),
                                                        SavedFilteredPopupChatUsers
                                                                .removeUserFromList(
                                                                        context,
                                                                        popUp));
                                    }
                                    setIconMode(MODES.MAIN); //reset view icon mode to default
                                }
                            })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // do nothing
                                }
                            })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void deleteAllContacts() {
        new AlertDialog.Builder(MapView.getMapView().getContext())
                .setTitle("Delete All")
                .setMessage("Remove All Users?")
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                // continue with delete
                                SavedFilteredPopupChatUsers.saveNewUserList(
                                        context, SavedFilteredPopupChatUsers
                                                .getDefault());
                                setIconMode(MODES.MAIN); //reset view icon mode to default
                            }
                        })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                // do nothing
                            }
                        })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void addUsers() {
        addUsersLayout.setVisibility(View.VISIBLE);
        listingLayout.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
        deleteAll.setVisibility(View.GONE);
        backImageButton.setVisibility(View.VISIBLE);
        add.setVisibility(View.GONE);
        ok.setVisibility(View.VISIBLE);
        sort.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        adding = true;
        noSavedUsersTextView.setVisibility(View.GONE);
        setIndividualList(); //default list
    }

    /**
     * handles the sorting functions allowing the current adapter to
     * sort the string lists from A-Z currently
     */
    private void sortUsers() {
        chatPopFilterAdapter.sortAZ();
        delete.setVisibility(View.GONE);
        deleteAll.setVisibility(View.VISIBLE);
        if (chatPopFilterAdapter.isListSorted())
            sort.setVisibility(View.GONE);
    }

    /**
     * the main first UI to display when defaulting the current view structure
     */
    private void setUpMainUI() {
        setStates();
        listingLayout.setVisibility(View.VISIBLE);
        addUsersLayout.setVisibility(View.GONE);
        backImageButton.setVisibility(View.VISIBLE);
        titleTextView.setVisibility(View.VISIBLE);
        search.setVisibility(View.VISIBLE);
        searchEditText.setVisibility(View.GONE);
        add.setVisibility(View.VISIBLE);
        adding = false;
        searchStatus = false;
        sort.setVisibility(View.VISIBLE);
        ok.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
        newAdapterInstance(context);
        filterListView.setAdapter(chatPopFilterAdapter);
        if (chatPopFilterAdapter.getCount() == 0) {
            noSavedUsersTextView.setText("No Saved Users");
            sort.setVisibility(View.GONE);
            search.setVisibility(View.GONE);
            deleteAll.setVisibility(View.GONE);
        } else {
            deleteAll.setVisibility(View.VISIBLE);
        }
        if (chatPopFilterAdapter.isListSorted())
            sort.setVisibility(View.GONE);

        //remove the adapter set so dropdown can close properly
        addUsersListView.setAdapter(null);
    }


    /**
     * handles the searching enum filter dropdown state
     * showing an edittext and pinging the current adapter for a matching regex
     */
    private void searchUsers() {
        searchEditText.setText(""); //default the list just in case if there is some garbage left over
        backImageButton.setVisibility(View.VISIBLE);
        searchEditText.setVisibility(View.VISIBLE);
        searchEditText.requestFocus();
        add.setVisibility(View.GONE);
        titleTextView.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        sort.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
        deleteAll.setVisibility(View.GONE);
        searchStatus = true;
    }

    /**
     * creates the contact list adapter instance
     * we are reusing the standard implementation to allow
     * the adapter to update itself instead of trying to recode something that
     * already exists
     */
    private void setIndividualList() {
        containerLayout.setBackgroundColor(Color.BLACK);
        contactListAdapter = new SingleAdapter(context);
        addUsersListView.setOnItemLongClickListener(null); //clear out listener
        checkList();
    }

    private void checkList() {
        if (!checkForContacts()) {
            Log.d(TAG, "No Users in Adapter");
            //no single users :: alert user
            addUsersListView.setAdapter(null);
            noUsersConnectedTextView.setVisibility(View.VISIBLE);
        } else {
            //set adapter if there are single contacts to display
            noUsersConnectedTextView.setVisibility(View.GONE);
            if (contactListAdapter != null)
                addUsersListView.setAdapter(contactListAdapter);
        }
    }

    /**checks the current contacts instance class for all single
     * user contacts and returns if list > 0
     * @return single contacts > 0
     */
    private boolean checkForContacts() {
        List<String> contacts = Contacts.getInstance()
                .getAllIndividualContactUuids();
        return contacts.size() > 0;
    }

    /**
     * a custom adapter that display all groups
     * when a user selects a group the adapter get the child groups or contacts
     * so a user can select specific groups
     */
    private void setGroupList() {
        noUsersConnectedTextView.setVisibility(View.GONE);
        addUsersAdapter = new AddUsersAdapter(context, this);
        contactListAdapter = null;
        addUsersListView.setAdapter(addUsersAdapter);
    }

    /**
     * forces the soft keyboard to close itself it showing
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getRoot().getWindowToken(), 0);
    }

    /**
     * Called when this intent is started by adding to the ATAK broadcast receiver helper class
     * android searches for any classes that listen for this intent and calls onRecieve()
     *
     * @param context the activity context
     * @param intent  the intent
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Received event: " + intent.getAction());
        if (intent.getAction().equals(
                "com.atakmap.android.CONTACTS_CHANGED_EVENT")) {
            reloadAdapters();
        } else if (intent.getAction().equals(
                "com.atakmap.android.FILTER_USERS_POPUPS")) {
            if (!isVisible()) {
                show(root);
            } else {
                PluginHelper.showDropDownExists();
            }
        }
    }

    private void reloadAdapters() {
        if (root != null) {
            //make sure this is even available
            if (addUsersListView.getAdapter() instanceof SingleAdapter) {
                //we are looking at the individual listings update it!
                setIndividualList();
            } else if (addUsersListView.getAdapter() instanceof AddUsersAdapter) {
                setGroupList();
            } else if (addUsersListView.getAdapter() instanceof TeamsAdapter) {
                if (teamsAdapter != null) {
                    teamsAdapter.notifyDataSetChanged();
                }
            } else if (addUsersListView.getAdapter() instanceof UserGroupsAdapter) {
                if (userGroupsAdapter != null) {
                    userGroupsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    /** Displays the toast that contains the tooltips sent in for dropdown icons
     * Example: user selects the ADD icon, toast shows "add A user To The List
     */
    private void showToolTipToast(String string) {
        Toast.makeText(MapView.getMapView().getContext(), string,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * attaches long click listeners to the menu icons for the filter chat users dropdown
     * if user has pref set up then attach these to the specific menu image buttons specified!
     */
    private void setLongClickers() {
        backImageButton.setOnLongClickListener(longClicker);
        search.setOnLongClickListener(longClicker);
        delete.setOnLongClickListener(longClicker);
        add.setOnLongClickListener(longClicker);
        sort.setOnLongClickListener(longClicker);
        ok.setOnLongClickListener(longClicker);
        titleTextView.setOnLongClickListener(longClicker);
    }

    /**
     * handles what message based on widget id to post in the toast message
     */
    private final View.OnLongClickListener longClicker = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.backImageButton:
                    if (searchStatus) {
                        showToolTipToast("Return To Filter List");
                    } else {
                        showToolTipToast(tips[4]);
                    }
                    break;
                case R.id.deleteFilterUserImageButton:
                    showToolTipToast(tips[1]);
                    break;
                case R.id.addFilterUserImageButton:
                    showToolTipToast(tips[0]);
                    break;
                case R.id.sortImageButton:
                    showToolTipToast(tips[2]);
                    break;
                case R.id.searchFilterImageButton:
                    showToolTipToast(tips[3]);
                    break;
                case R.id.chatFilterTitleTextView:
                    showToolTipToast(tips[5]);
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    /**
     * Binds the XMl variable objects to java wrapper objects
     * used to interact with the current view
     *
     * @param root the inflated view
     */
    private void createJavaElements(View root) {

        containerLayout = (RelativeLayout) root
                .findViewById(R.id.containerRelativeLayout);
        backImageButton = (ImageButton) root.findViewById(R.id.backImageButton);
        searchEditText = (EditText) root.findViewById(R.id.searchEditText);
        noSavedUsersTextView = (TextView) root
                .findViewById(R.id.noSavedUsersTextView);
        filterListView = (ListView) root.findViewById(R.id.filterUsersListView);
        titleTextView = (TextView) root
                .findViewById(R.id.chatFilterTitleTextView);
        add = (ImageButton) root.findViewById(R.id.addFilterUserImageButton);
        delete = (ImageButton) root
                .findViewById(R.id.deleteFilterUserImageButton);
        search = (ImageButton) root.findViewById(R.id.searchFilterImageButton);
        titleTextView = (TextView) root
                .findViewById(R.id.chatFilterTitleTextView);
        sort = (ImageButton) root.findViewById(R.id.sortImageButton);
        ok = (ImageButton) root.findViewById(R.id.okButton);
        listingLayout = (LinearLayout) root
                .findViewById(R.id.listingLinearLayout);
        addUsersLayout = (LinearLayout) root
                .findViewById(R.id.addUsersLinearLayout);
        addUsers = (Button) root.findViewById(R.id.individualButton);
        addGroup = (Button) root.findViewById(R.id.groupsButton);
        addUsersListView = (ListView) root.findViewById(R.id.addUsersListView);
        noUsersConnectedTextView = (TextView) root
                .findViewById(R.id.noUsersListTextView);
        deleteAll = (ImageButton) root.findViewById(R.id.deleteAllButton);
    }

    /**
     * Creates a new adapter when creating the list of users
     * or when a specific change happens the adapter needs to be re made
     */
    private void newAdapterInstance(final Context context) {

        chatPopFilterAdapter = new ChatPopFilterAdapter(context,
                new ItemsChecked() {
                    @Override
                    public void itemsChecked(boolean checked) {
                        delete.setVisibility(checked ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void noItemsInSearch() {
                        TextView tv = new TextView(context);
                        tv.setLayoutParams(new LinearLayout.LayoutParams
                                (ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT));
                        tv.setText("No Users Found!");
                        tv.setTextAppearance(
                                context,
                                android.R.style.TextAppearance_DeviceDefault_Large);
                        tv.setGravity(Gravity.CENTER_HORIZONTAL);
                        listingLayout.addView(tv, 0);
                        chatPopFilterAdapter.clear();
                    }
                });
    }

    /**
     * determine the number of entries currently in list
     * adjust view types on action bar to if/else a specific number
     * HIDE/UNHIDE views
     */

    private void setStates() {
        int numberOfEnteries = SavedFilteredPopupChatUsers
                .getEntireUserListPopUpUsers(context).size();
        delete.setVisibility(numberOfEnteries > 0 ? View.VISIBLE : View.GONE);
        search.setVisibility(numberOfEnteries > 0 ? View.VISIBLE : View.GONE);
        noSavedUsersTextView.setVisibility(numberOfEnteries == 0 ? View.VISIBLE
                : View.GONE);
        sort.setVisibility(numberOfEnteries > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * @param root the view to show inside the dropdown
     *             size is scaled larger on non tablet devices width in landscape mode to fit entire contents
     */
    private void show(View root) {
        showDropDown(root, PluginHelper.isDeviceTablet() ? FIVE_TWELFTHS_WIDTH
                : HALF_WIDTH,
                FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT,
                false, this);
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {

    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {

    }

    @Override
    public void onDropDownVisible(boolean v) {
        setIconMode(MODES.MAIN); //default
    }

    /**Displays toast message to user
    * showing number of users added and other error messages
    */
    private void disbatchAddedUsersMessage(int added, int alreadyAdded) {
        //tell user how many contacts were added
        if (added > 0) {
            Toast.makeText(
                    MapView.getMapView().getContext()
                    ,
                    "Added "
                            + added
                            + " contacts "
                            + (alreadyAdded > 0 ?
                                    alreadyAdded + " User"
                                            + (alreadyAdded == 1 ? "" : "s")
                                            + " Already In List" : ""),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(
                    MapView.getMapView().getContext()
                    ,
                    "No new contacts added "
                            + (alreadyAdded > 0 ?
                                    alreadyAdded + " User"
                                            + (alreadyAdded == 1 ? "" : "s")
                                            + " Already In List" : ""),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private View getRoot() {
        return root;
    }

    private void setRoot(View root) {
        this.root = root;
    }
}
