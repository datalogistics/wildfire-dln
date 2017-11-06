package com.gotenna.atak.plugin.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.gui.HintDialogHelper;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.bbn.roger.encryption.AndroidEncryptionUtils;
import com.gotenna.atak.plugin.R;
import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.bluetooth.BluetoothAdapterManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.exceptions.GTDataMissingException;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.messages.GTBaseMessageData;
import com.gotenna.sdk.messages.GTMessageData;
import com.gotenna.sdk.messages.GTTextOnlyMessageData;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.responses.SystemInfoResponseData;
import com.gotenna.sdk.types.GTDataTypes;
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.atakmap.android.maps.MapEvent.ITEM_PERSIST;
import static com.atakmap.android.maps.MapEvent.ITEM_SHARED;

public class GotennaDropDownReceiver extends DropDownReceiver {

    public static final String TAG = "GotennaDropDownReceiver";
    public final int MAXIMUM_LENGTH_OF_MESSAGE = 160;
    public static final String SHOW_UI_INTENT_ACTION = "com.gotenna.atak.plugin.plugin.OPEN_TOOL";
    private static final int LOC_TYPE=0; //???
    public static final String CHAT_TEXT_HISTORY = "chatTextHistory";
    public static final String QUICK_CHAT_BUTTONS = "quickChatButtons";
    public static final String QUICK_CHAT_TEXT_PREFIX = "quickChatFullTextPrefix-";
    public static final String CHAT_DELIM = ": ";
    public static final Map<String, String> quickChatDefaults = new HashMap<String, String>();
    public static String[] quickChatDefaultKeys = { "RGR", "@LCC", "@VDO", "@Brch" };
    static {
        quickChatDefaults.put(quickChatDefaultKeys[0], "Roger");
        quickChatDefaults.put(quickChatDefaultKeys[1], "at LCC");
        quickChatDefaults.put(quickChatDefaultKeys[2], "at VDO");
        quickChatDefaults.put(quickChatDefaultKeys[3], "at breach");
    }
    public static final int SCAN_TIMEOUT = 25000; // 25 sec

    private Context pluginContext;
    private Context activityContext;
    private static View toolView;
    private static String chatTextHistory = "";
    private Handler handler;
    private static ProgressDialog progressDialog;
    private static ImageButton settingsButton;
    private static Button scanForGoTennaButton;
    private static TextView chatHistoryTextView;
    private static Button sendMessageButton;
    private static EditText messageEditText;
    private static ScrollView chatHistoryScrollView;
    private static TextView batteryLevelTextView;

    protected GotennaDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
    }

    @Override
    protected void disposeImpl() {
        updateQuickChatButtonPrefs();
    }

    private void saveChatHistory() {
        SharedPreferences prefs = getPrefs();
        Log.d(TAG, "Saving " + chatTextHistory.length() + " bytes of chat history");
        prefs.edit().putString(CHAT_TEXT_HISTORY, chatTextHistory).commit();
        Log.d(TAG, "Saved " + chatTextHistory.length() + " bytes of chat history");
    }

    public AtomicBoolean isRunning = new AtomicBoolean(false);
    public AtomicBoolean shouldRun = new AtomicBoolean(true);
    private Thread broadcastLocationThread = new Thread() {
        @Override
        public void run() {

            // Start-up the Dissemination Service
            DisseminationService.getInstance().start(getMapView().getContext());

            // Start-up my periodic updates, including:
            //  * Blue Force Tracks
            //  * Battery display updates
            isRunning.set(true);
            while(shouldRun.get()) {
                try {

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) { /* ignore this */ }

                    // Broadcast Location (if enabled)
                    SharedPreferences prefs = getPrefs();
                    boolean shouldBroadcastLocation = prefs.getBoolean(GotennaSettingsDropDownReceiver.BROADCAST_LOCATION, true);
                    if(shouldBroadcastLocation) {
                        if (getMapView() != null && getMapView().getSelfMarker() != null) {
                            byte[] mapEventBytes = GotennaCodec.serializePointMapItemV0(GotennaDropDownReceiver.this.getMapView().getSelfMarker());
                            Log.d(TAG, "BFT msg serialized to " + mapEventBytes.length + " bytes");
                            encryptAndSendBFT(mapEventBytes, getMapView().getSelfMarker().getPoint());
                        }
                    }

                    // Update Battery Level
                    GTCommandCenter.getInstance()
                            .sendGetSystemInfo(new GTCommandCenter.GTSystemInfoResponseListener() {
                                                   @Override
                                                   public void onResponse(SystemInfoResponseData systemInfoResponseData) {
                                                       final int batt = systemInfoResponseData.getBatteryLevelAsPercentage();
                                                       final long battLevel = systemInfoResponseData.getBatteryLevel();
                                                       //final int batt = voltageToPercentage((int)battLevel);
                                                       Log.d(TAG, "battLevel: " + battLevel + "  " + batt + "%");
                                                       ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               batteryLevelTextView.setText(batt + "%");
                                                               batteryLevelTextView.setBackgroundColor(Color.BLUE);
                                                           }
                                                       });
                                                   }
                                               }, new GTErrorListener() {
                                                   @Override
                                                   public void onError(GTError gtError) {
                                                       ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               detectedDisconnect();
                                                           }
                                                       });
                                                   }
                                               }
                            );

                } catch (Exception e) {
                    Log.w(TAG, "Broadcast Location Thread caught unexpected error: ", e);
                }
            }

            // Show that we're not running anymore if we somehow escape from the loop
            isRunning.set(false);
        }
    };

    private void detectedDisconnect() {
        batteryLevelTextView.setText("X");
        batteryLevelTextView.setBackgroundColor(Color.RED);
        scanForGoTennaButton.setText("Sync");
        scanForGoTennaButton.setOnClickListener(scanButtonListener);
    }

    private void onBroadcastLocError(GTError gtError) {
        Log.w(TAG, "Error broadcasting location: " + gtError);
    }

    private void onBroadcastLocSuccess(GTResponse gtResponse) {
        Log.d(TAG, "Broadcasted location successfully.");
    }

    private View.OnClickListener scanButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Ensuring that we're disconnected...");
            GTConnectionManager.getInstance().disconnect();

            Log.d(TAG, "Starting GoTenna Scan...");
            GTConnectionManager.getInstance().addGtConnectionListener(scanListener);
            startPairing();
        }
    };

    private void initializeGuiElements() {

        setupProgressDialog();

        settingsButton = ((ImageButton)toolView.findViewById(R.id.settings_button));
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Clicked Settings Button - open GoTenna settings");
                // Send an Intent to open the Settings DropDown
                Intent openToolIntent = new Intent(GotennaSettingsDropDownReceiver.SHOW_SETTINGS_UI_INTENT_ACTION);
                activityContext.sendBroadcast(openToolIntent);
            }
        });

        scanForGoTennaButton = ((Button)toolView.findViewById(R.id.scanForGotennaButton));
        scanForGoTennaButton.setOnClickListener(scanButtonListener);

        batteryLevelTextView = ((TextView)toolView.findViewById(R.id.battery_level_text));

        SharedPreferences prefs = getPrefs();
        chatTextHistory = prefs.getString(CHAT_TEXT_HISTORY, "");

        chatHistoryTextView = ((TextView)toolView.findViewById(R.id.chat_history_text));
        chatHistoryTextView.setText(chatTextHistory);

        chatHistoryScrollView = ((ScrollView)toolView.findViewById(R.id.scrollView));
        chatHistoryScrollView.fullScroll(ScrollView.FOCUS_DOWN);

        messageEditText = ((EditText)toolView.findViewById(R.id.messageText));
        // limit the length of the text available to send (so our full callsign can also be transmitted)
        //TODO: eliminate this constraint when we maintain a goTenna "contact list" on the client side
        messageEditText.setFilters(
                new InputFilter[] {
                        new InputFilter.LengthFilter(
                                MAXIMUM_LENGTH_OF_MESSAGE-(getMapView().getDeviceCallsign().length() + ": ".length())
                        )});

        sendMessageButton = ((Button)toolView.findViewById(R.id.sendMessageButton));
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // prepend my callsign to text
                final String myCallsign = getMapView().getDeviceCallsign();
                final String msgText = messageEditText.getText().toString().trim();
                if(msgText.isEmpty()) {
                    // Don't bother sending anything if it's blank
                    return;
                }
                final String senderPlusMessage = myCallsign + CHAT_DELIM + msgText;
                String chatPayload = senderPlusMessage;
                if(GotennaEncryptionSettingsDropDownReceiver.getInstance().shouldEncrypt()) {
                    try {
                        chatPayload = new String(
                                Base64.encode(
                                        GotennaEncryptionSettingsDropDownReceiver.getInstance()
                                                .encrypt(senderPlusMessage.getBytes()), Base64.DEFAULT));
                    } catch (Exception e) {
                        Log.w(TAG, "Error encrypting chat message: ", e);
                    }
                }
                final String finalChatPayload = chatPayload;
                GTTextOnlyMessageData msg = null;
                try {
                    msg = new GTTextOnlyMessageData(finalChatPayload);
                } catch (GTDataMissingException e) {
                    Log.w(TAG, "Error making text message", e);
                }

                if(msg == null || GTCommandCenter.getInstance() == null) {
                   showToast("Could not send message. Try reconnecting.");
                   return;
                }

                // clear the message out of the input box; this should hopefully prevent the duplicate message problems
                messageEditText.setText("");

                // TODO: add it to the history immediately, then show somehow that it was sent
                DisseminationService.getInstance().sendChat(new DisseminationService.Message(msg.serializeToBytes(), new DisseminationService.OnMessageSentCallback() {
                    @Override
                    public void onMessageSent() {
                        ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messageEditText.setText("");
                                String myMsg = dateFormat.format(new Date()) + " Me: " + msgText;
                                chatTextHistory += myMsg + "\n";
                                Log.d(TAG, "Appending chat to: " + chatHistoryTextView);
                                chatHistoryTextView.setText(chatTextHistory);
                                chatHistoryScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                saveChatHistory();
                            }
                        });
                    }

                    @Override
                    public void onSendError(final String error) {
                        ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getMapView().getContext(), "goTenna chat error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }));

            }
        });

        Set<String> quickChatButtons = prefs.getStringSet(QUICK_CHAT_BUTTONS, null);
        String[] quickChatButtonArray = new String[4];
        if(quickChatButtons != null) {
            quickChatButtons.toArray(quickChatButtonArray);
        }

        final Button quickChat1 = ((Button)toolView.findViewById(R.id.quickChat1));
        try {
            String shortCode = quickChatButtonArray[0];
            if(shortCode == null) shortCode = quickChatDefaultKeys[0];
            quickChat1.setText(shortCode);
        } catch (Exception e) { /* ignore */ }
        quickChat1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageEditText.append(getFullTextForQuickChat(quickChat1.getText()) + " ");
            }
        });
        quickChat1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openQuickChatEditorDialog(quickChat1);
                return true;
            }
        });

        final Button quickChat2 = ((Button)toolView.findViewById(R.id.quickChat2));
        try {
            String shortCode = quickChatButtonArray[1];
            if(shortCode == null) shortCode = quickChatDefaultKeys[1];
            quickChat2.setText(shortCode);
        } catch (Exception e) { /* ignore */ }
        quickChat2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageEditText.append(getFullTextForQuickChat(quickChat2.getText()) + " ");
            }
        });
        quickChat2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openQuickChatEditorDialog(quickChat2);
                return true;
            }
        });


        final Button quickChat3 = ((Button)toolView.findViewById(R.id.quickChat3));
        try {
            String shortCode = quickChatButtonArray[2];
            if(shortCode == null) shortCode = quickChatDefaultKeys[2];
            quickChat3.setText(shortCode);
        } catch (Exception e) { /* ignore */ }
        quickChat3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageEditText.append(getFullTextForQuickChat(quickChat3.getText()) + " ");
            }
        });
        quickChat3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openQuickChatEditorDialog(quickChat3);
                return true;
            }
        });


        final Button quickChat4 = ((Button)toolView.findViewById(R.id.quickChat4));
        try {
            String shortCode = quickChatButtonArray[3];
            if(shortCode == null) shortCode = quickChatDefaultKeys[3];
            quickChat4.setText(shortCode);
        } catch (Exception e) { /* ignore */ }
        quickChat4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageEditText.append(getFullTextForQuickChat(quickChat4.getText()) + " ");
            }
        });
        quickChat4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openQuickChatEditorDialog(quickChat4);
                return true;
            }
        });

        try {
            // Need to use a special context because the library calls context.getApplicationContext() often
            GoTenna.setApplicationToken(new WrappedPackageContext(pluginContext, getMapView().getContext()), GotennaLifecycle.GOTENNA_APP_TOKEN);
        } catch (Exception e) {
            Log.w(TAG, "Problem w/ GoTenna Init.", e);
        }

    }

    private class WrappedPackageContext extends ContextWrapper {
        Context appContext;
        WrappedPackageContext(Context packageContext, Context appContext) {
            super(packageContext);
            this.appContext = appContext;
        }

        @Override
        public Context getApplicationContext() {
            return new WrappedPackageContext(pluginContext, appContext);
        }

        @Override
        public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
            return appContext.openFileOutput(name, mode);
        }

        @Override
        public FileInputStream openFileInput(String name) throws FileNotFoundException {
            return appContext.openFileInput(name);
        }

        @Override
        public Object getSystemService(String service) {
            return appContext.getSystemService(service);
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return appContext.getSharedPreferences(name, mode);
        }

    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Got intent to open GoTenna DropDown");

        this.activityContext = context;
        this.handler = new Handler(Looper.getMainLooper());

        if(toolView == null) {
            Log.d(TAG, "Inflating GUI");
            final LayoutInflater inflater = (LayoutInflater) pluginContext.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            toolView = inflater.inflate(R.layout.tool_layout, null);
            initializeGuiElements();
            Log.d(TAG, "Done inflating GUI");
        }

        Log.d(TAG, "Showing the GoTenna GUI");
        showDropDown(toolView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, false);

        // show hint at startup
        // attempting to add a hint when the plugin first starts
        // showHint method defined below

        showHint();

        // Now set-up a listener for items that are being sent off the device
        MapEventDispatcher dispatcher = getMapView().getMapEventDispatcher();
        //dispatcher.addMapEventListener(ITEM_PERSIST, mapEventDispatchListenerForSharingOverGotenna);
        //dispatcher.addMapEventListener(MapEvent.ITEM_SHARED, mapEventDispatchListenerForSharingOverGotenna);
        dispatcher.addMapEventListener(mapEventDispatchListenerForSharingOverGotenna);
        Log.d(TAG, "Attached Map Event listener for GoTenna sharing");

    }

    // showHint method that presents users with a tool tip at startup
    // basic information provided below, feel free to modify

    public void showHint() {
        HintDialogHelper
                .showHint(
                        getMapView().getContext(),
                        "goTenna Plugin",
                        "This tool allows users to network using the goTenna." +
                                "Users can share blue force tracks, points, and chat with other TAK goTenna users." +
                                "If a user is also connected to TAKServer, goTenna traffic is shared with non-goTenna users. \n\n" +
                                "Enhanced network encryption can be configured in the Settings (gear icon) > Encryption Options.",
                        "gotenna.tool");
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(getMapView().getContext());
        progressDialog.setMessage("Searching for GoTenna...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                GTConnectionManager.GTConnectionState connectionState =
                        GTConnectionManager.getInstance().getGtConnectionState();

                // The progress dialog was dismissed while we were in the middle of scanning for the goTenna
                if (connectionState == GTConnectionManager.GTConnectionState.SCANNING)
                {
                    stopScanning();
                    showToast("Cancelled GoTenna connection");
                }
            }
        });
    }

    private final Runnable scanTimeoutRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            stopScanning();
            progressDialog.dismiss();
            new AlertDialog.Builder(getMapView().getContext())
                    .setTitle("Could not pair with goTenna")
                    .setMessage("Syncing with the goTenna failed. Do you want to forget any previously-connected goTenna?")
                    .setPositiveButton("Forget Previous goTenna", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                GTConnectionManager.getInstance().clearConnectedGotennaAddress();
                                Log.d(TAG, "Cleared previous GoTenna Address");
                            } catch (Exception e) {
                                Log.w(TAG, "Got unexpected exception clearing GoTenna Address", e);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // do nothing.
                        }
                    })
                    .create()
                    .show();
        }
    };

    private void stopScanning() {
        handler.removeCallbacks(scanTimeoutRunnable);
        GTConnectionManager.getInstance().disconnect();
    }

    private View quickChatEditorDialogView = null;

    private void openQuickChatEditorDialog(final Button quickChat) {
        final LayoutInflater inflater = (LayoutInflater) pluginContext.getSystemService(pluginContext.LAYOUT_INFLATER_SERVICE);
        quickChatEditorDialogView = inflater.inflate(R.layout.quick_chat_edit, null);
        final EditText shortCode = (EditText) quickChatEditorDialogView.findViewById(R.id.short_code);
        shortCode.setText(quickChat.getText());
        final EditText longText  = (EditText) quickChatEditorDialogView.findViewById(R.id.long_text);
        longText.setText(getFullTextForQuickChat(quickChat.getText()));
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getMapView().getContext())
                .setTitle("Enter Button Config")
                .setView(quickChatEditorDialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // First make the changes to the GUI
                        quickChat.setText(shortCode.getText());
                        // Next, commit the button's shared-prefs
                        SharedPreferences prefs = getPrefs();
                        prefs.edit().putString(QUICK_CHAT_TEXT_PREFIX+shortCode.getText(),
                                longText.getText().toString()).commit();
                        updateQuickChatButtonPrefs();
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        dialogBuilder.create().show();
    }

    private void updateQuickChatButtonPrefs() {
        if(toolView == null) {
            Log.w(TAG, "Trying to update quick chat buttons, but the view hasn't been instantiated yet.");
            return;
        }
        final Button quickChat1 = ((Button)toolView.findViewById(R.id.quickChat1));
        final Button quickChat2 = ((Button)toolView.findViewById(R.id.quickChat2));
        final Button quickChat3 = ((Button)toolView.findViewById(R.id.quickChat3));
        final Button quickChat4 = ((Button)toolView.findViewById(R.id.quickChat4));
        Set<String> qcButtons = new HashSet<String>();
        qcButtons.add(quickChat1.getText().toString());
        qcButtons.add(quickChat2.getText().toString());
        qcButtons.add(quickChat3.getText().toString());
        qcButtons.add(quickChat4.getText().toString());
        SharedPreferences prefs = getPrefs();
        prefs.edit().putStringSet(QUICK_CHAT_BUTTONS, qcButtons).commit();
    }

    private CharSequence getFullTextForQuickChat(CharSequence shortCode) {
        SharedPreferences prefs = getPrefs();
        String longText = prefs.getString(QUICK_CHAT_TEXT_PREFIX+shortCode, null);
        if(longText == null) {
            longText = quickChatDefaults.get(shortCode);
        }
        if(longText == null) {
            longText = "";
        }
        return longText;
    }

    void showToast(final String msg) {
        ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getMapView().getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    boolean scanListenerAdded = false;

    GTConnectionManager.GTConnectionListener scanListener = new GTConnectionManager.GTConnectionListener() {
        @Override
        public void onConnectionStateUpdated(GTConnectionManager.GTConnectionState gtConnectionState) {
            if (gtConnectionState == GTConnectionManager.GTConnectionState.CONNECTED) {
                Log.d(TAG, "GoTenna Connected!");
                handler.removeCallbacks(scanTimeoutRunnable);
                progressDialog.dismiss();
                onConnectedToGT();
            } else if (gtConnectionState == GTConnectionManager.GTConnectionState.SCANNING) {
                Log.d(TAG, "GoTenna Scanning!");
            } else {
                Log.d(TAG, "Problem connecting GoTenna: " + gtConnectionState);
            }
        }
    };

    private long getGid() {
        long toobig = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        String tooManyDigits = ""+toobig;
        String rightSize = tooManyDigits.substring(0,10);
        return Long.parseLong(rightSize);
    }

    private void onConnectedToGT() {

        SharedPreferences prefs = getPrefs();
        boolean rememberPrevUser = prefs.getBoolean(GotennaSettingsDropDownReceiver.REMEMBER_PREVIOUS_USER, false);
        if(!rememberPrevUser) {
            UserDataStore.getInstance().deleteCurrentUser();
        }

        final User user = UserDataStore.getInstance().getCurrentUser();
        if(user == null) {
            Log.d(TAG, "No current GoTenna User");
            final String myCallsign = getMapView().getDeviceCallsign();
            final long gid = getGid();
            GTCommandCenter.getInstance().setGoTennaGID(gid, myCallsign, new GTCommand.GTCommandResponseListener() {
                @Override
                public void onResponse(final GTResponse gtResponse) {
                    ((Activity) getMapView().getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(gtResponse.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE) {
                                Log.d(TAG, "Set GoTenna GID for " + myCallsign + " to " + gid);
                                //((TextView) toolView.findViewById(R.id.userinfotextview)).setText(myCallsign + ", " + gid);
                            } else {
                                showToast("Error setting GID");
                                //((TextView) toolView.findViewById(R.id.userinfotextview)).setText("Error setting GID");
                            }
                        }
                    });
                }
            }, new GTErrorListener() {
                @Override
                public void onError(final GTError gtError) {
                    ((Activity) getMapView().getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //((TextView) toolView.findViewById(R.id.userinfotextview)).setText(gtError.toString());
                            showToast("Connection error: " + gtError);
                        }
                    });
                }
            });
        } else {
            Log.d(TAG, "Re-using GoTenna User:" + user);
//            ((Activity) getMapView().getContext()).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ((TextView) toolView.findViewById(R.id.userinfotextview)).setText(user.name + ", " + user.gId);
//                }
//            });
        }

        GTCommandCenter.getInstance().setMessageListener(gtMessageListener);

        if(!isRunning.get()) {
            shouldRun.set(true);
            broadcastLocationThread.start();
        }

        detectedConnection();
    }

    private void detectedConnection() {
        scanForGoTennaButton.setText("Ping");
        scanForGoTennaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GTCommandCenter.getInstance().sendEchoCommand(new GTCommand.GTCommandResponseListener() {
                    @Override
                    public void onResponse(final GTResponse gtResponse) {
                        if (gtResponse.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE) {
                            showToast("Ping Success!");
                        } else {
                            showToast("Ping error: " + gtResponse.toString());
                        }
                    }
                }, new GTErrorListener() {
                    @Override
                    public void onError(GTError gtError) {
                        showToast("Ping error: " + gtError.toString());
                        detectedDisconnect();
                    }
                });
            }
        });
    }

    private int notificationId = 823722;
    private void showNotification(String title, String text) {
        NotificationUtil.getInstance().postNotification(notificationId,
                       NotificationUtil.GeneralIcon.CHAT.getID(),
                       title, null, text);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd kk:mm:ss");

    private GTCommandCenter.GTMessageListener gtMessageListener = new GTCommandCenter.GTMessageListener() {
        @Override
        public void onIncomingMessage(GTMessageData gtMessageData) {
            Log.d(TAG, "onIncomingMessage (not base)");
            byte[] bytesToParse = gtMessageData.getDataToProcess();
            Log.d(TAG, "incoming message is " + bytesToParse.length + " bytes");
            Log.d(TAG, "incoming message bytes: " + GotennaCodec.bytesToHex(bytesToParse));
            CotEvent gtCotEvent = deserializeCotMessageFromGoTenna(bytesToParse);
            if(gtCotEvent == null) {
                Log.d(TAG, "could not deserialize plain-text CoT message... trying to decrypt");
                gtCotEvent = AndroidEncryptionUtils.decryptAndDeserialize(bytesToParse);
            }
            if(gtCotEvent != null) {
                Log.d(TAG, "deserialized CoT message, sharing via proxy...");
                CotMapComponent.getInternalDispatcher().dispatch(gtCotEvent);
                CotMapComponent.getExternalDispatcher().dispatch(gtCotEvent);
            } else {
                Log.d(TAG, "could not decrypt/deserialize CoT message...");
            }
        }

        @Override
        public void onIncomingMessage(final GTBaseMessageData gtBaseMessageData) {
            Log.d(TAG, "onIncomingMessage (base)");

            final String text = AndroidEncryptionUtils.decryptChatMsg(gtBaseMessageData.getText());
            // this will only decrypt if it is encrypted to begin with

            if(text == null) return; // i.e., if we didn't have the key to decrypt it

            //final String sender = gtBaseMessageData.getSenderInitials();
            final Date sentAtTimeDate = gtBaseMessageData.getMessageSentDate();
            final String sentAtTime = dateFormat.format(sentAtTimeDate);
            if(text != null) {
                Log.d(TAG, "GoTenna msg: " + text);

                //Toast.makeText(activityContext, sender + ": " + text, Toast.LENGTH_LONG).show();
                showNotification("ATAK goTenna Chat", text);

                ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String myMsg = sentAtTime + " " + text;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                chatTextHistory += myMsg + "\n";
                                Log.d(TAG, "Appending chat to: " + chatHistoryTextView);
                                chatHistoryTextView.setText(chatTextHistory);
                                chatHistoryScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                saveChatHistory();
                            }
                        }, 500);
                    }
                });

                /* What an All Chat looks like...
                <event version="2.0" uid="GeoChat.ANDROID-990005899005707.All Chat Rooms.03bd4923-9f33-41a9-b41e-a1b9d3d038e9"
                        type="b-t-f" time="2016-12-16T16:09:29.886Z" start="2016-12-16T16:09:29.886Z" stale="2016-12-17T16:09:29.886Z"
                        how="h-g-i-g-o">
                    <point lat="0.0" lon="0.0" hae="9999999.0" ce="9999999" le="9999999"/>
                    <detail>
                        <__chat senderCallsign="PROVO" chatroom="All Chat Rooms" groupOwner="false" id="All Chat Rooms" parent="RootChatGroup">
                            <chatgrp uid0="ANDROID-990005899005707" uid1="All Chat Rooms" id="All Chat Rooms"/>
                            <paths/>
                        </__chat>
                        <link relation="p-p" type="a-f-G-U-C" uid="ANDROID-990005899005707"/>
                        <remarks time="2016-12-16T16:09:29.886Z" to="All Chat Rooms" source="BAO.F.ATAK.ANDROID-990005899005707">Roger</remarks>
                        <__serverdestination destinations="192.1.19.209:4242:tcp:ANDROID-990005899005707"/>
                        <precisionlocation geopointsrc="???" altsrc="???"/>
                    </detail>
                </event>*/
                /*
                "<event version='2.0' uid='"+chat_msg_uid+"'"
                + "type='b-t-f' time='"+time+"' start='"+time+"' stale='"+stale+"'"
                + "how='h-g-i-g-o'>"
                + "<point lat='0.0' lon='0.0' hae='9999999.0' ce='9999999' le='9999999'/>"
                + "<detail>"
                + "<__chat senderCallsign='"+callsign+"' chatroom='All Chat Rooms' groupOwner='false' id='All Chat Rooms' parent='RootChatGroup'>"
                + "<chatgrp uid0='"+uid+"' uid1='All Chat Rooms' id='All Chat Rooms'/>"
                + "<paths/>"
                + "</__chat>"
                + "<link relation='p-p' type='a-f-G-U-C' uid='"+uid+"'/>"
                + "<remarks time='"+time+"' to='All Chat Rooms' source='BAO.F.ATAK."+uid+"'>"+text+"</remarks>"
                //+ "<__serverdestination destinations="192.1.19.209:4242:tcp:ANDROID-990005899005707"/>"
                + "<precisionlocation geopointsrc='???' altsrc='???'/>"
                + "</detail>"
                + "</event>";
                */

            } else {
                Log.d(TAG, "No text data in message");
            }

            /*
            GTLocationMessageData loc = gtBaseMessageData.getLocationMessageData();
            if (loc != null) {
                String name = loc.getName();
                String callsign = name;
                String groupName = null;
                if(name.contains(DELIM)) {
                    groupName = name.split(DELIM)[1];
                    Log.d(TAG, "group is " + groupName);
                    callsign = name.split(DELIM)[0];
                }
                double lat = loc.getLatitude();
                double lon = loc.getLongitude();
                Log.d(TAG, callsign + " @ location : " + lat + ", " + lon);

                CotEvent gtCotEvent = new CotEvent();
                gtCotEvent.setUID("gotenna-" + callsign);
                gtCotEvent.setType("a-f-G-U-C-I");
                CoordinatedTime time = new CoordinatedTime();
                gtCotEvent.setTime(time);
                gtCotEvent.setStart(time);
                gtCotEvent.setStale(time.addMinutes(10));
                gtCotEvent.setHow("h-e");
                gtCotEvent.setPoint(new CotPoint(lat, lon, CotPoint.COT_ALTITUDE_UNKNOWN, CotPoint.COT_CE90_UNKNOWN, CotPoint.COT_LE90_UNKNOWN));
                CotDetail taskDetail = new CotDetail("detail");
                CotDetail contactDetail = new CotDetail("contact");
                contactDetail.setAttribute("callsign", callsign);
                taskDetail.addChild(contactDetail);
                CotDetail summaryDetail = new CotDetail("summary");
                summaryDetail.setInnerText("goTenna");
                taskDetail.addChild(summaryDetail);
                CotDetail uidDetail = new CotDetail("uid");
                uidDetail.setAttribute("Droid", callsign);
                taskDetail.addChild(uidDetail);
                CotDetail groupDetail = new CotDetail("__group");
                groupDetail.setAttribute("name", groupName==null ? "Cyan" : groupName);
                taskDetail.addChild(groupDetail);
                gtCotEvent.setDetail(taskDetail);

                CotMapComponent.getInternalDispatcher().dispatch(gtCotEvent);
                CotMapComponent.getExternalDispatcher().dispatch(gtCotEvent);
            } else {
                Log.d(TAG, "No location data in message");
            }
            */
        }

//        @Override
//        public void onBusyResponseReceived() {
//            Log.d(TAG, "onBusyResponseReceived");
//        }
    };

    public SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getMapView().getContext());
    }

    private void startPairing() {
        BluetoothAdapterManager.BluetoothStatus bt_status = BluetoothAdapterManager.getInstance().getBluetoothStatus();
        if(bt_status == BluetoothAdapterManager.BluetoothStatus.SUPPORTED_AND_ENABLED) {

            String connectedGtAddress = GTConnectionManager.getInstance().getConnectedGotennaAddress();
            Log.d(TAG, "Remembered GT Address: " + connectedGtAddress);

            SharedPreferences prefs = getPrefs();
            boolean rememberPrevAddr = prefs.getBoolean(GotennaSettingsDropDownReceiver.REMEMBER_PREVIOUS_ADDR, true);
            if (!rememberPrevAddr) {
                Log.d(TAG, "Clearing previous GoTenna Address - will search for a new one");
                try {
                    GTConnectionManager.getInstance().clearConnectedGotennaAddress();
                    Log.d(TAG, "Cleared previous GoTenna Address");
                } catch (Exception e) {
                    Log.w(TAG, "Got unexpected exception clearing GoTenna Address", e);
                }
            }

            int gotennaTypeInt = prefs.getInt(GotennaSettingsDropDownReceiver.GOTENNA_TYPE, -1);
            GTConnectionManager.GTDeviceType gtDeviceType = GTConnectionManager.GTDeviceType.V1;
            if (gotennaTypeInt == 1) {
                gtDeviceType = GTConnectionManager.GTDeviceType.V1;
            } else if (gotennaTypeInt == 2) {
                gtDeviceType = GTConnectionManager.GTDeviceType.MESH;
            } else {
                gtDeviceType = GTConnectionManager.GTDeviceType.V1;
            }

            // Setting device type
            Log.d(TAG, "Setting goTenna device type to: " + gtDeviceType);
            GTConnectionManager.getInstance().setDeviceType(gtDeviceType);

            Log.d(TAG, "Starting Scan & Connect");
            GTConnectionManager.getInstance().scanAndConnect();
            // scanListener will be notified asynchronously

            progressDialog.show();
            handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT);

        } else {
            Log.w(TAG, "Bluetooth is not supported or enabled");
            showToast("Enable Bluetooth and Try Again");
        }
    }

    private int voltageToPercentage(int milliVolts) {
       // use the line: y = 424.16x + 3623.4
       double ret = (milliVolts - 3623.4) / 424.16 * 100.0;
       if(ret > 100.0) ret = 100.0;
       if(ret < 0.0) ret = 0.0;
       return (int) ret;
    }

    private byte[] serializeForGoTenna(MapEvent mapEvent) {
        return GotennaCodec.serializeForGoTenna(mapEvent);
    }

    public static CotEvent deserializeCotMessageFromGoTenna(byte[] bytesToParse) {
        // read first short to find out what type of message this is...
        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(new ByteArrayInputStream(bytesToParse));
            return GotennaCodec.parsePointMapItemV0(inputStream);
//            short dataType = inputStream.readShort();
//            if(dataType == GotennaCodec.GT_MSG_POINT_MAP_ITEM_V1) {
//                return GotennaCodec.parsePointMapItemV1(inputStream);
//            } else if (dataType == GotennaCodec.GT_MSG_RECTANGLE_V1) {
//                return GotennaCodec.parseDrawingRectangleV1(inputStream);
//            } else if (dataType == GotennaCodec.GT_MSG_POLYLINE_V1) {
//                return GotennaCodec.parsePolylineV1(inputStream);
//            } else {
//                Log.w(TAG, "Unhandled data type: " + dataType);
//            }
        } catch (Exception ioe) {
            Log.w(TAG, "Exception parsing incoming message.", ioe);
        }

        return null;
    }

    private void encryptAndSend(byte[] clearTextMsg, final String callsignOfMapItem) {
        byte[] mapEventBytes = null;

        if(GotennaEncryptionSettingsDropDownReceiver.getInstance().shouldEncrypt()) {
            try {
                Log.d(TAG, "Encrypting goTenna message!");
                mapEventBytes = GotennaEncryptionSettingsDropDownReceiver.getInstance().encrypt(clearTextMsg);
            } catch (Exception e) {
                Log.w(TAG, "Problem encrypting data", e);
                return;
            }
        } else { // if encryption is OFF
            mapEventBytes = clearTextMsg;
        }

        if(mapEventBytes != null) {
            Log.d(TAG, callsignOfMapItem + " w/ encryption serialized to " + mapEventBytes.length + " bytes");
            DisseminationService.getInstance().sendCoT(new DisseminationService.Message(mapEventBytes, new DisseminationService.OnMessageSentCallback() {
                @Override
                public void onMessageSent() {
                    Log.d(TAG, "Sent goTenna Msg: " + callsignOfMapItem);
                }

                @Override
                public void onSendError(final String error) {
                    Log.w(TAG, "Error sending goTenna message: " + error);
                    ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getMapView().getContext(), "goTenna error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }));
        } else {
            Log.d(TAG, "no message to send");
        }
    }

    private void encryptAndSendBFT(byte[] clearTextMsg, final GeoPoint geoPoint) {
        byte[] mapEventBytes = null;

        if(GotennaEncryptionSettingsDropDownReceiver.getInstance().shouldEncrypt()) {
            try {
                Log.d(TAG, "Encrypting goTenna message!");
                mapEventBytes = GotennaEncryptionSettingsDropDownReceiver.getInstance().encrypt(clearTextMsg);
            } catch (Exception e) {
                Log.w(TAG, "Problem encrypting data", e);
                return;
            }
        } else { // if encryption is OFF
            mapEventBytes = clearTextMsg;
        }

        if(mapEventBytes != null) {
            Log.d(TAG, "BFT w/ encryption serialized to " + mapEventBytes.length + " bytes");
            DisseminationService.getInstance().sendBFT((new DisseminationService.BlueForceTrack(mapEventBytes, new DisseminationService.OnMessageSentCallback() {
                @Override
                public void onMessageSent() {
                    Log.d(TAG, "Sent goTenna BFT");
                }

                @Override
                public void onSendError(String error) {
                    Log.w(TAG, "Error sending goTenna BFT: " + error);
                }
            }, geoPoint)));
        } else {
            Log.d(TAG, "no message to send");
        }
    }

    private MapEventDispatcher.MapEventDispatchListener mapEventDispatchListenerForSharingOverGotenna = new MapEventDispatcher.MapEventDispatchListener() {
        @Override
        public void onMapEvent(final MapEvent mapEvent) {
            // throw away all map* events immediately
            if(mapEvent.getType().startsWith("map")) return;

            Log.d(TAG, "got an " + mapEvent.getType() + " map event...");
            //Log.d(TAG, " `- extras: " + mapEvent.getExtras());
            //Log.d(TAG, " `- from:   " + mapEvent.getFrom());

            // only send out persists & shares
            if(!mapEvent.getType().equals(ITEM_PERSIST) && !mapEvent.getType().equals(ITEM_SHARED))
                return;

            // filter only outgoing messages on PERSIST messages
            if(mapEvent.getType().equals(ITEM_PERSIST) && mapEvent.getExtras().getBoolean("internal",true))
                return;

            // filter out messages that are sent any way other than broadcast (e.g., p2p, group, team)
            String[] connectStrings = mapEvent.getExtras().getStringArray("toConnectStrings");
            if (connectStrings != null && connectStrings.length > 0) {
                Log.d(TAG, "This message was sent to an individual or group. Don't broadcast it over goTenna.");
                return;
//                for(String connectStr : connectStrings)
//                    Log.d(TAG, " - Send target: " + connectStr);
            }

            // TODO: only try to send when connected...

            final String callsignOfMapItem = mapEvent.getItem().getMetaString("callsign",
                    mapEvent.getItem().getMetaString("shapeName", null));
            Log.d(TAG, "Sending item " + callsignOfMapItem + " over goTenna...");

            byte[] mapEventBytes = serializeForGoTenna(mapEvent);
            if(mapEventBytes != null) {
                Log.d(TAG, callsignOfMapItem + " serialized to " + mapEventBytes.length + " bytes");

                encryptAndSend(mapEventBytes, callsignOfMapItem);
            }

        }
    };

}
