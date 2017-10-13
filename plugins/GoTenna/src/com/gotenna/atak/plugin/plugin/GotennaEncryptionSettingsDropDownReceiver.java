package com.gotenna.atak.plugin.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.bbn.roger.encryption.AndroidEncryptionUtils;
import com.bbn.roger.encryption.EncryptionUtils;
import com.bbn.roger.encryption.Key;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.gotenna.atak.plugin.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * Created by kusbeck on 12/14/16.
 */
public class GotennaEncryptionSettingsDropDownReceiver extends DropDownReceiver implements DropDown.OnStateListener {

    public static final String TAG = GotennaEncryptionSettingsDropDownReceiver.class.getSimpleName();
    public static final String SHOW_ENCRYPTION_SETTINGS_UI_INTENT_ACTION = "SHOW_ENCRYPTION_SETTINGS_UI_INTENT_ACTION";

    public static final String ENCRYPT_PREF = "encrypt";
    public static final String SELECTED_KEY_PREF = "selected_gotenna_encryption_key";

    private View toolView;
    private Context pluginContext;
    private Context activityContext;
    private ArrayAdapter<String> encryptionKeyListViewAdapter;
    private TextView selectedKeyTextView;
    private String mostRecentlySelectedKey = null;

    private static GotennaEncryptionSettingsDropDownReceiver _instance = null;

    public static GotennaEncryptionSettingsDropDownReceiver initialize(MapView mapView, final Context context) {
        _instance = new GotennaEncryptionSettingsDropDownReceiver(mapView, context);
        return _instance;
    }

    public static GotennaEncryptionSettingsDropDownReceiver getInstance() {
        return _instance;
    }

    private GotennaEncryptionSettingsDropDownReceiver(MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
    }

    @Override
    protected void disposeImpl() {

    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getMapView().getContext());

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.activityContext = context;

        Log.d(TAG, "Inflating Encryption Settings GUI");
        final LayoutInflater inflater = (LayoutInflater) pluginContext.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        toolView = inflater.inflate(R.layout.encryption_layout, null);

        showDropDown(toolView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH, HALF_HEIGHT, this);

        // Get a handle on the shared preferences
        final SharedPreferences prefs = getPrefs();
        mostRecentlySelectedKey = null;

        final Switch encryptSwitch = ((Switch)toolView.findViewById(R.id.encryption_switch));
        encryptSwitch.setChecked(prefs.getBoolean(ENCRYPT_PREF, false));
        encryptSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SharedPreferences prefs = getPrefs();
                prefs.edit()
                        .putBoolean(ENCRYPT_PREF, encryptSwitch.isChecked())
                        .apply();
            }
        });

        final ListView encryptionKeyListView = ((ListView)toolView.findViewById(R.id.encryption_key_list));
        encryptionKeyListViewAdapter = new ArrayAdapter<String>(
                activityContext, android.R.layout.simple_list_item_1, AndroidEncryptionUtils.getFiles(AndroidEncryptionUtils.KEY_LOCATION_ON_SDCARD));
        encryptionKeyListView.setAdapter(encryptionKeyListViewAdapter);

        encryptionKeyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String key = encryptionKeyListView.getItemAtPosition(i).toString();
                Log.d(TAG, "Item clicked: "  + key);
                mostRecentlySelectedKey = key;
                encryptionKeyListView.setSelection(i);
            }
        });

        /*
        encryptionKeyListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Selected Key: " + view);
                view.setSelected(true);
                String selectedKeyPref = encryptionKeyListView.getItemAtPosition(i).toString();
                prefs.edit().putString(SELECTED_KEY_PREF, selectedKeyPref).apply();
                selectedKeyTextView.setText("Encrypting with " + selectedKeyPref);
                return true;
            }
        });
        */

        selectedKeyTextView = ((TextView)toolView.findViewById(R.id.selected_key_textview));
        String selectedKeyPref = prefs.getString(SELECTED_KEY_PREF, null);
        if(selectedKeyPref == null) {
            selectedKeyTextView.setText("Select your encryption key.");
        } else {
            selectedKeyTextView.setText("Encrypting with " + selectedKeyPref);
        }

        final Button selectKeyButton = ((Button)toolView.findViewById(R.id.select_key_button));
        selectKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "selecting key: " + mostRecentlySelectedKey);
                if(mostRecentlySelectedKey != null) {
                    String selectedKeyPref = mostRecentlySelectedKey;
                    prefs.edit().putString(SELECTED_KEY_PREF, selectedKeyPref).apply();
                    selectedKeyTextView.setText("Encrypting with " + selectedKeyPref);
                }
            }
        });

        final Button removeKeyButton = ((Button)toolView.findViewById(R.id.remove_key_button));
        removeKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "removing key: " + mostRecentlySelectedKey);
                if(mostRecentlySelectedKey != null) {

                    // If they try to remove the key they're encrypting with...
                    if(mostRecentlySelectedKey.equals(prefs.getString(SELECTED_KEY_PREF, null))) {
                        new AlertDialog.Builder(getMapView().getContext())
                                .setTitle("Disable Encryption?")
                                .setMessage("You are trying to remove a key that you're using to encrypt. Removing it will disable encryption.")
                                .setPositiveButton("Disable Encryption", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        prefs.edit().putString(SELECTED_KEY_PREF, null).apply();
                                        selectedKeyTextView.setText("Select your encryption key.");
                                        prefs.edit().putBoolean(ENCRYPT_PREF, false).apply();
                                        encryptSwitch.setChecked(false);
                                        dialogInterface.dismiss();

                                        // now another dialog...
                                        new AlertDialog.Builder(getMapView().getContext())
                                                .setTitle("Remove " + mostRecentlySelectedKey + "?")
                                                .setMessage("The key will still be saved on the file system, but no longer available for encryption or decryption")
                                                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        try {
                                                            AndroidEncryptionUtils.archiveKey(mostRecentlySelectedKey);
                                                            // refresh the GUI?
                                                            encryptionKeyListViewAdapter.remove(mostRecentlySelectedKey);
                                                            encryptionKeyListViewAdapter.notifyDataSetChanged();
                                                            // key is no longer selected
                                                            mostRecentlySelectedKey = null;
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Problem moving key", e);
                                                        }
                                                    }
                                                })
                                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.cancel();
                                                    }
                                                })
                                                .show();

                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                    }
                                })
                                .show();
                    } else {

                        new AlertDialog.Builder(getMapView().getContext())
                                .setTitle("Remove " + mostRecentlySelectedKey + "?")
                                .setMessage("The key will still be saved on the file system, but no longer available for encryption or decryption")
                                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            AndroidEncryptionUtils.archiveKey(mostRecentlySelectedKey);
                                            // refresh the GUI?
                                            encryptionKeyListViewAdapter.remove(mostRecentlySelectedKey);
                                            encryptionKeyListViewAdapter.notifyDataSetChanged();
                                            // key is no longer selected
                                            mostRecentlySelectedKey = null;
                                        } catch (Exception e) {
                                            Log.e(TAG, "Problem moving key", e);
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                    }
                                })
                                .show();
                    }
                }
            }
        });

        final Button shareKeyButton = ((Button)toolView.findViewById(R.id.share_key_button));
        shareKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mostRecentlySelectedKey != null) {
                    String qrCodeText = encodeAsQRCodeText(mostRecentlySelectedKey);
                    ImageView imageView = new ImageView(getMapView().getContext());
                    try {
                        Bitmap bitmap = encodeAsBitmap(qrCodeText, 500);
                        imageView.setImageBitmap(bitmap);
                        new AlertDialog.Builder(getMapView().getContext())
                                .setTitle("Key as QR Code")
                                .setView(imageView)
                                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                    }
                                })
                                .show();
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getMapView().getContext(), "Select a key", Toast.LENGTH_SHORT).show();
                }
            }
        });


        final Button addKeyButton = ((Button)toolView.findViewById(R.id.add_key_button));
        addKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new AlertDialog.Builder(getMapView().getContext())
                        .setTitle("Add Key")
                        .setMessage("Select the mechanism to use to add a key")
                        .setPositiveButton("Generate New", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final EditText keyNameInputText = new EditText(getMapView().getContext());
                                new AlertDialog.Builder(getMapView().getContext())
                                        .setTitle("Name Key")
                                        .setView(keyNameInputText)
                                        .setPositiveButton("Create Key", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                try {
                                                    String keyName = keyNameInputText.getText() + ".key";
                                                    EncryptionUtils.generateKey(AndroidEncryptionUtils.KEY_LOCATION_ON_SDCARD + "/" + keyName);
                                                    Log.d(TAG, "Created a new key named " + keyName);
                                                    // add to gui
                                                    encryptionKeyListViewAdapter.add(keyName);
                                                    encryptionKeyListViewAdapter.notifyDataSetChanged();
                                                } catch (IOException ioe) {
                                                    Log.w(TAG, "Problem creating new key", ioe);
                                                }
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.cancel();
                                            }
                                        })
                                        .show();
                            }
                        })
                        .setNegativeButton("QR Code", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                final ZXingScannerView mScannerView = new ZXingScannerView(pluginContext);
                                final AlertDialog qrGui = new AlertDialog.Builder(getMapView().getContext())
                                        .setTitle("Scan QR Code")
                                        .setView(mScannerView)
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                mScannerView.stopCamera();
                                                dialogInterface.cancel();
                                            }
                                        })
                                        .create();

                                mScannerView.startCamera();
                                mScannerView.setResultHandler(new ZXingScannerView.ResultHandler() {
                                    @Override
                                    public void handleResult(Result result) {
                                        try {
                                            final Key key = decodeFromQRCodeText(result.getText());
                                            Log.d(TAG, "Got QR Result: " + key.getName() + " with " + key.getBase64decodedKey().length + " byte key");
                                            qrGui.cancel();
                                            final EditText keyNameInputText = new EditText(getMapView().getContext());
                                            if(key.getName() != null) {
                                                keyNameInputText.setText(key.getName());
                                            }
                                            new AlertDialog.Builder(getMapView().getContext())
                                                    .setTitle("Enter Key Name")
                                                    .setView(keyNameInputText)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            try {
                                                                key.setName(keyNameInputText.getText().toString());
                                                                Toast.makeText(getMapView().getContext(), "Adding key " + key.getName(), Toast.LENGTH_SHORT).show();
                                                                File keyFile = new File(AndroidEncryptionUtils.KEY_LOCATION_ON_SDCARD + "/" + key.getName());
                                                                FileOutputStream fos = new FileOutputStream(keyFile);
                                                                EncryptionUtils.writeKeyToFile(key.getBase64decodedKey(), fos);
                                                                encryptionKeyListViewAdapter.add(key.getName());
                                                                encryptionKeyListViewAdapter.notifyDataSetChanged();
                                                            } catch (Exception ex) {
                                                                Log.w(TAG, "Error writing QR Code Key", ex);
                                                            }
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.cancel();
                                                        }
                                                    })
                                                    .show();
                                        } catch (Exception e) {
                                            Log.w(TAG, "QR Code Reader error", e);
                                        }
                                    }
                                });

                                qrGui.show();
                            }
                        })
                        .show();

            }
        });

        /*
        final Button removeKeyButton = ((Button)toolView.findViewById(R.id.remove_key_button));
        removeKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
         */

    }

    public static final String QRCODE_DELIM = "XXX";
    private String encodeAsQRCodeText(String mostRecentlySelectedKey) {
        String ret = null;
        try {
            byte[] keyBytes = EncryptionUtils.readKeyFromFile(mostRecentlySelectedKey);
            String keyString = Base64.encodeToString(keyBytes, Base64.DEFAULT);
            ret = mostRecentlySelectedKey + QRCODE_DELIM + keyString;
        } catch (Exception e) {
            Log.w(TAG, "Error encoding key as QR Code", e);
        }
        return ret;
    }

    private Key decodeFromQRCodeText(String qrCodeText) {
        String[] parts = qrCodeText.split(QRCODE_DELIM);
        if(parts.length < 2) {
            return new Key(Base64.decode(qrCodeText, Base64.DEFAULT));
        } else {
            Log.d(TAG, "before key base64 decode: " + parts[1]);
            Log.d(TAG, " after key base64 decode: " + Base64.decode(parts[1], Base64.DEFAULT));
            return new Key(Base64.decode(parts[1], Base64.DEFAULT), parts[0]);
        }
    }

    private Bitmap encodeAsBitmap(String qrCodeText, int size) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(qrCodeText,
                    BarcodeFormat.QR_CODE, size, size, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, size, 0, 0, w, h);
        return bitmap;
    }

    public boolean shouldEncrypt() {
        SharedPreferences prefs = getPrefs();
        return prefs.getBoolean(GotennaEncryptionSettingsDropDownReceiver.ENCRYPT_PREF, false)
                && (prefs.getString(GotennaEncryptionSettingsDropDownReceiver.SELECTED_KEY_PREF, null) != null);
    }

    public byte[] encrypt(byte[] clearTextMsg) throws Exception {
        SharedPreferences prefs = getPrefs();
        String keyFileName = prefs.getString(GotennaEncryptionSettingsDropDownReceiver.SELECTED_KEY_PREF, null);
        byte[] keyToEncryptWith = EncryptionUtils.readKeyFromFile(keyFileName);
        return EncryptionUtils.encrypt(keyToEncryptWith, clearTextMsg);
    }

    void showToast(final String msg) {
        ((Activity)getMapView().getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getMapView().getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDropDownSelectionRemoved() {

    }

    @Override
    public void onDropDownClose() {
        // Send an Intent to open the DropDown
        Intent openToolIntent = new Intent(GotennaSettingsDropDownReceiver.SHOW_SETTINGS_UI_INTENT_ACTION);
        this.activityContext.sendBroadcast(openToolIntent);
    }

    @Override
    public void onDropDownSizeChanged(double v, double v1) {

    }

    @Override
    public void onDropDownVisible(boolean b) {

    }
}
