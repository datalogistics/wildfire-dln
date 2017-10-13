    Quick Chat
    _________________________________________________________________
    PURPOSE AND CAPABILITIES

    Specific README For QuickChat
    version 1.0 By Scott Auman 8/16/2016 Updated: 4/1/2017

   QuickChat  - connects with ATAK core and allows incoming chat messages to
   display a large dialog banner for users who request seeing certain/specific messages from high Priority Users. Users are determined based on a predefined list
   built into the plugin with ability to be added/deleted to the list. When users are contained in the filter list any messages incoming from that user are displayed
   as a MessagePopup.

   Each user added is added from the current contact list, no user can be added to the list unless they are currently online. Users added to list then go offline are stored  in memory
   if any user changes there call signs when they are online the call signs contained in the list are updated automatically.

   Adding a user to the filter list: (will allow messages from this user to appear as popup dialogs)
   Action Bar Overflow menu. -->QuickChat Tools --> Dialog will appear with 2 options. Select the user's list.

   A dropdown with appear from the right with the title "Chat Message Filter List". This will display all users currently bound
    by callsign in the current list of users to show chat messages popups. If you would like to add a user they
    must be currently online connected to your WIFI or your TAk server. Clicking the add users icon at the top of the dropdown will open a window
    with the listing of the current users connected to your tak server. The individual button shows all users by callsign connected. The Groups button will display your
    current custom chat groups as well as the team groups.

    To add a user(s) from the individual listing checkmark the box next to their name(s).
    When done selecting the users click the checkmark icon at the top of the dropdown.
    This will bring you back to the original filter list containing the newly added users.

   To add a user(s) from Chat Groups. Select the groups button tab. YOu can add users from team groups(teams) or custom user created groups.
   Each groups will display how many users are online currently in that group. Long pressing on a group name will display the user(s) callsigns that
   are contained in that group. To add all users from the group, checkmark the group name and select the checkmark icon. This will add the users to the filter list
   and bring you back to the main filter list dropdown

   To remove user(s) from this list you can select the user checkbox to the left of the name. and hit the delete user icon at the top of the drop down.

   ***see Complication for more info about plugin lifecycle and major components****

  Preferences:

  There are 1 main preference and 9 sub preferences for this plugin currently.
  Sub preferences are not activated unless the chat message preference is enabled.
  Main preference is enabled by default on plugin installation.

  Enable message Popups - enables the user of popups being constructed for users in the filter ist currently bound.
  Banner Screen Location - Sets the location where the banner will appear. Currently supporting TOP and CENTER
  Banner color theme - Sets the Color theme to use for the banner. Currently supporting BLACK / WHITE
  Quick Reply button text - display's a dialog that contains the text selected to use in the quick repky button of the chat popup dialog. ADD QUICK TEXT button
       allows you to enter a custom text to the list to be selectable.
   Use 24 Hour Time - banner time stamp will display in 24 hour format.
   Enlarge message text - banner message text will display a highest setting. NOTE: long messages will have to be scrolled in the container.
   Vibrate Device- (optional on some devices) vibrates the device when pop message is received
   Mark messages read - If enabled this will mark the message read state and remove the unread display for that particular message.
   Clear message history on ATAK exit. - when exiting ATAK if enabled all chat history will be cleared.
   Export History - allows you to create a json,xml,csv file and export it out to a folder on the device for saving current history.
   Plugin Documentation - a guide to how functions work and setup instruction for using this plugin.

  Message POPUPS:

  The message popup is a subclasses from the Android dialog instance class. The banner appears from the top and animated down.
  The banner does not inhibit the map in case the user is working with the map. The banner contains a listing view of

                              ---------------------------------------------------------------------------------------------------
                              -     (call sign)                                      (Message Number / Total In Back Stack    -
                              -                                                                                                      (date)           -
                              -    (message)                                                                                    (Time)          -
                              -----------------------------------------------------------------------------------------------------


  Dialogs are shown one at a time. Any persisting message that yield a pop up message are created and put into a back stack class(ChatMessagePopups).
  When user dismisses the current banner the class looks to see if any dialogs are in the back stacks, sorts them by the time they were received and displays the next one in line
  if any exist!

  Instant Reply Message Feature.

     ---------------------------------------------------------------------------------------------------
    -     (call sign)                                      (Message Number / Total In Back Stack    -
    -                                                                                                      (date)           -
    -    (message)                                                                                    (Time)          -

    ---------------------------                 ------------------------------------------------------------
    -                               -                 -                                                                         -
    -          REPLY           -                 -                   CUSTOM REPLY TEXT                   -
    ----------------------------                -------------------------------------------------------------

                                              DISMISS
      -----------------------------------------------------------------------------------------------------


    This feature allows a user to send a reply back without having to enter chat and type a message.

    Reply Button - Allows you to type a quick small message to send back to the user
    Send Button - Sends the supplied text to the user.

    Dismiss Button - Closes the current dialog

    Custom Text Button - (see preferences to set custom text). Clicking this will send the text contained in this button to the user immediately.
    and close the dialog. This allows a user to send a predefined message acknowledging the message was received. This can be setup in the preferences.

   Message History:
   Every chat message coming into the plugin as a popup will store itself in a history module. Users can navigate to the history module by selecting the tool descriptor   on the action bar.
  This brings a drop down window(right) that lists each stored message in history(currently defaults to earliest-latest sort method) Users can review messages received.

  Messages are persisted through ATAK instances unless proper preference is set to delete/clear all history on ATAK exit.
  NOTE::: history only includes messages as popups IE: messages received from users that were placed on the list to receive those messages.
  NOTE::: See in application help file Settings->Tools->QuickChat->Help
    _________________________________________________________________
    STATUS

    Released to SOMPE September 1st 2016

    _________________________________________________________________
    ATAK VERSIONS

    ATAK 3.4,3.5

    _________________________________________________________________
    POINT OF CONTACTS

    Primary Developer Contact:     Scott Auman  scott_auman@partech.com / 1-910-396-6176  / PAR Government - SOMPE
    PAR Contact:           Eric Donovan.  Eric_Donovan@partech.com / 919-285-5546 / PAR Government - SOMPE
    SOMPE Program Office Contact:  Christopher Abbot.  christopher.d.abbott8.civ@mail.mil / 757-878-6659 / SOMPE

    _________________________________________________________________
    USER GROUPS

    SOF,SOAR,USASOC,MARSOC

    _________________________________________________________________
    EQUIPMENT REQUIRED

    Active Server connection to receive chat messages from point to point(user to user).
    User's connected to a TAK server.

    _________________________________________________________________
    EQUIPMENT SUPPORTED

    _________________________________________________________________
    COMPILATION

    QuickChat Tool - connects with ATAK core and allows incoming chat messages to
    display a large dialog banner for users who request seeing certain/specific messages from high Priority Users. Users are determined based on a predefined list
    built into the plugin.

    Private Plugins offer the most capability for utilizing the ATAK subsystem, but
    this interface will likely change from version to version.

    build.xml and Makefile both reflect the same project name (in this case QuickChat).

    The assets file describes both a Lifecycle and a ToolDescriptor.   For convention,
    these are in the same location used in the AndroidManifest.xml file.    For
    readability I have broken out the plugin to be in a directory off of the main
    package structure.

    When constructing the plugin, it is important to recognize that there are two
    different android.content.Context in play.

  The plugin context is used to resolve resources from the plugin APK
  The mapView context is used for graphic access (AlertDialogs, Toasts, etc).

    Note:
   The plugin context will cause a runtime error to occur if used to construct an
   AlertDialog.

    _________________________________________________________________
    DEVELOPER NOTES

    All persisted data that is not preferences are stored in json key-value pairs as full json strings.
    Saved plugin data persists with ATAK meaning you can uninstall the plugin and still have user settings saved.

    The plugin contains a preference plugin that display’s specific preferences for the message banner popups
    The preference is registered using the toolfragment.register static method and given a string key and applied the shortcut settings
    When the plugin is loaded the lifecycle plugin class handles loading any overlays that are reached into the ATAK core.
    The Tool Descriptor handles the state of the action bar icon for this plugin, the On Click method contained in the ToolDescriptor superclass handles when the
    action bar icon is pressed.

    For this plugin the intent that contains the filterdropdownreceiver is called and the ATAkBroadcast class sends the intent out to all receivers
    the intent string we supplied matches with the receiver called and calls onReceiver in the FilterChatUserDropDown class
    This creates the drop down used to display the listing of the users that are currently filtered to receive popup message banners from if they receive the chat messages
    The dropdown contains the standard lib of add/delete/sort/search icons used to interact with the list. The dropdown is linked to the specific settings page for the plugin while the dropdown is activated
    so if a user has the drop down open. Then select settings... the plugin settings are loaded without having to navigate through the hierarchy.

    The user list is stored dynamically using a JSON formatted notation. Each user is a custom Object based off of the original Contacts object in ATAK core.
    Each user is stored with their current callings string and uid string. The uid string is vital to changing and updating the call signs.
