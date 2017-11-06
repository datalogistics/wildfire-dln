package com.atakmap.android.takchat.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

import com.atakmap.android.gui.PanEditTextPreference;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.network.ui.CredentialsPreference;
import com.atakmap.android.preference.PluginPreferenceFragment;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.data.ChatDatabase;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.app.ATAKActivity;
import com.atakmap.app.preferences.NetworkConnectionPreferenceFragment;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;

import org.jxmpp.jid.BareJid;

import java.io.File;

/**
 * TAK Chat preference fragment
 */
public class TAKChatPreferenceFragment extends PluginPreferenceFragment {

    private final static String TAG = "TAKChatPreferenceFragment";

    private static Context _staticPluginContext;
    private Context context;

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public TAKChatPreferenceFragment() {
         super(_staticPluginContext, R.xml.tools_preferences);
    }


    @SuppressWarnings("ValidFragment")
    public TAKChatPreferenceFragment(final Context pluginContext) {
          super(pluginContext, R.xml.tools_preferences);
          _staticPluginContext = pluginContext;
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", _staticPluginContext.getString(R.string.takchat_prefs));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext());

        final PanEditTextPreference takchatServerPortPref = (PanEditTextPreference)findPreference("takchatServerPort");

        final Preference caLocation = findPreference(TAKChatUtils.getPluginString(R.string.caChatTrustStore));
        caLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        NetworkConnectionPreferenceFragment.getCertFile(
                                context,
                                "Chat Trust Store (CA)",
                                TAKChatUtils.getPluginString(R.string.caChatTrustStore),
                                false, null);
                        return false;
                    }
                });


        final Preference takchatClearHistory = findPreference("takchatClearHistory");
        takchatClearHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context)
                        .setIcon(com.atakmap.app.R.drawable.ic_menu_delete_32)
                        .setTitle("Confirm Delete")
                        .setMessage("Remove chat message history from local device?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d(TAG, "Clearing chat history");

                                Intent upIntent = new Intent(context, ATAKActivity.class);
                                NavUtils.navigateUpTo((Activity) context, upIntent);

                                AtakBroadcast.getInstance().sendBroadcast(new Intent(TAKChatDropDownReceiver.CLEAR_HISTORY));
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return false;
            }
        });

        final Preference takchatExportHistory = findPreference("takchatExportHistory");
        takchatExportHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context)
                        .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                        .setTitle("Confirm Export")
                        .setMessage("Export chat history from local device?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d(TAG, "Exporting ing chat history");
                                //TODO get off UI thread?

                                Intent upIntent = new Intent(context, ATAKActivity.class);
                                NavUtils.navigateUpTo((Activity) context, upIntent);

                                try {
                                    File file = new File(FileSystemUtils.getItem(FileSystemUtils.EXPORT_DIRECTORY),
                                            "XmppChatDb-" + new CoordinatedTime()
                                                    .getMilliseconds()
                                                    + "-export.csv");
                                    ChatDatabase.getInstance(TAKChatUtils.pluginContext).exportHistory(file
                                            .getAbsolutePath());
                                    Toast.makeText(TAKChatUtils.mapView.getContext(),
                                            getString(com.atakmap.app.R.string.chat_text16) + file,
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Log.w(TAG,
                                            "Error exporting chat history from XMPP Chat DB",
                                            e);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return false;
            }
        });


        final Preference takchatClearCredentials = findPreference("takchatClearCredentials");
        takchatClearCredentials
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(context)
                                .setIcon(com.atakmap.app.R.drawable.ic_menu_delete_32)
                                .setTitle("Confirm Delete")
                                .setMessage("Remove chat user credentials and certificates?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Log.d(TAG, "Clearing chat credentials and certificates");

                                        final String type = TAKChatUtils.getPluginString(R.string.xmpp_credentials);
                                        AtakCertificateDatabase.deleteCertificate(TAKChatUtils.getPluginString(R.string.caChatTrustStore));
                                        AtakAuthenticationDatabase.invalidate(type, type);

                                        AtakBroadcast.getInstance().sendBroadcast(
                                                new Intent(CredentialsPreference.CREDENTIALS_UPDATED).putExtra("type",
                                                        type));

                                        prefs.edit().remove("takchatServerPort").apply();

                                        takchatServerPortPref.setText("5222");
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();

                        return false;
                    }
                });

        final CheckBoxPreference takchatStoreLocally = (CheckBoxPreference) findPreference("takchatStoreLocally");
        takchatStoreLocally
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        new AlertDialog.Builder(context)
                                .setIcon(com.atakmap.app.R.drawable.ic_menu_delete_32)
                                .setTitle("Confirm Delete")
                                .setMessage("Remove chats already stored locally and disable local storage of future chats?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Log.d(TAG, "Store locally changed");
                                        //TODO once confirmed by user, update ChatDatabase settings
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();

                        return false;
                    }
                });


        final CheckBoxPreference takchatUseTakServerTrustStore = (CheckBoxPreference) findPreference("takchatUseTakServerTrustStore");
        takchatUseTakServerTrustStore.setEnabled(prefs.getBoolean("takchatRequireSSL", true));
        final Preference caChatTrustStore = findPreference("caChatTrustStore");
        caChatTrustStore.setEnabled(!prefs.getBoolean("takchatUseTakServerTrustStore", true));
        final Preference caChatPassword = findPreference("caChatPassword");
        caChatPassword.setEnabled(!prefs.getBoolean("takchatUseTakServerTrustStore", true));

        final CheckBoxPreference takchatRequireSSL = (CheckBoxPreference) findPreference("takchatRequireSSL");
        takchatRequireSSL
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, final Object newValue) {
                        final Boolean bNewValue = (Boolean) newValue;
                        if (bNewValue) {
                            Log.d(TAG, "takchatRequireSSL enabled");

                            takchatUseTakServerTrustStore.setEnabled(bNewValue);
                            caChatTrustStore.setEnabled(bNewValue && !prefs.getBoolean("takchatUseTakServerTrustStore", true));
                            caChatPassword.setEnabled(bNewValue && !prefs.getBoolean("takchatUseTakServerTrustStore", true));

                            return true;
                        } else {
                            new AlertDialog.Builder(context)
                                    .setIcon(com.atakmap.app.R.drawable.ic_lock_lit)
                                    .setTitle("Confirm Security Setting")
                                    .setMessage("When disabling this security setting TAK Chat will still attempt to establish an encrypted channel with the XMPP server. If that fails, then communications will be sent in clear text. If encryption is supported by the XMPP server, then TAK Chat will not perform TLS certificate validation. Do you understand the risk, and would you like to proceed with degraded security?")
                                    .setPositiveButton("Yes. Continue", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Log.d(TAG, "takchatRequireSSL disabled by user:");
                                            prefs.edit().putBoolean("takchatRequireSSL", false).apply();
                                            takchatRequireSSL.setChecked(false);

                                            takchatUseTakServerTrustStore.setEnabled(bNewValue);
                                            caChatTrustStore.setEnabled(bNewValue && !prefs.getBoolean("takchatUseTakServerTrustStore", true));
                                            caChatPassword.setEnabled(bNewValue && !prefs.getBoolean("takchatUseTakServerTrustStore", true));

                                            Log.d(TAG, "takchatRequireSSL user proceed insecure: " + newValue);
                                        }
                                    })
                                    .setNegativeButton("No. Stay Secure", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Log.d(TAG, "takchatRequireSSL No. Stay Secure");
                                        }
                                    })
                                    .show();

                            return false;
                        }
                    }
                });

        takchatUseTakServerTrustStore
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Boolean bNewValue = (Boolean)newValue;
                        Log.d(TAG, "takchatUseTakServerTrustStore: " + bNewValue);
                        caChatTrustStore.setEnabled(!bNewValue);
                        caChatPassword.setEnabled(!bNewValue);
                        return true;
                    }
                });

        final PanEditTextPreference takchatCustomHost = (PanEditTextPreference) findPreference("takchatCustomHost");
        takchatCustomHost.setEnabled(prefs.getBoolean("takchatUseCustomHost", false));

        final CheckBoxPreference takchatUseCustomHost = (CheckBoxPreference) findPreference("takchatUseCustomHost");
        takchatUseCustomHost
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Boolean bNewValue = (Boolean)newValue;
                        Log.d(TAG, "takchatUseCustomHost: " + bNewValue);
                        takchatCustomHost.setEnabled(bNewValue);

                        //initialize to current server for convenience
                        if(bNewValue) {
                            String customHost = prefs.getString("takchatCustomHost", null);
                            BareJid me = TAKChatUtils.getUsernameBare();
                            String server = null;
                            if(me != null && me.getDomain() != null)
                                server = me.getDomain().toString();

                            if (FileSystemUtils.isEmpty(customHost) && !FileSystemUtils.isEmpty(server)) {
                                takchatCustomHost.setText(server);
                                Log.d(TAG, "Init custom host: " + server);
                            }
                        }

                        return true;
                    }
                });
    }
}
