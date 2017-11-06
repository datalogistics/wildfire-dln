package com.bbn.roger.encryption;

import android.util.Base64;

import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.gotenna.atak.plugin.plugin.GotennaDropDownReceiver;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by kusbeck on 12/20/16.
 */

public class AndroidEncryptionUtils {

    public static final String TAG = AndroidEncryptionUtils.class.getSimpleName();
    public static final String KEY_LOCATION_ON_SDCARD = "/sdcard/atak/certs/gotenna";
    public static final String OLD_KEY_LOCATION_ON_SDCARD = "/sdcard/atak/certs/gotenna/old";

    private static List<Key> decryptionKeyList = new LinkedList<Key>();

    public static ArrayList<String> getFiles(String keyLocationOnSdcard) {
        ArrayList<String> myFiles = new ArrayList<String>();
        File f = new File(keyLocationOnSdcard);

        f.mkdirs();
        File[] files = f.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].isFile()) {
                myFiles.add(files[i].getName());
            }
        }
        return myFiles;
    }

    public static void populateDecryptionKeyList() {
        synchronized (decryptionKeyList) {
            decryptionKeyList.clear();
            ArrayList<String> fileList = getFiles(KEY_LOCATION_ON_SDCARD);
            for (String filename : fileList) {
                try {
                    decryptionKeyList.add(
                            new Key(EncryptionUtils.readKeyFromFile(filename), filename));
                } catch (Exception e) {
                    Log.w(TAG, "problem reading key in " + KEY_LOCATION_ON_SDCARD + "/" + filename, e);
                }
            }
        }
    }

    public static CotEvent decryptAndDeserialize(byte[] bytesToDecrypt) {
        CotEvent ret = null;
        try {
            populateDecryptionKeyList();
            synchronized (decryptionKeyList) {
                byte[] decryptedMsg = null;
                for(Key key : decryptionKeyList) {
                    try {
                        Log.d(TAG, "trying to decrypt with " + key.getName());
                        decryptedMsg = EncryptionUtils.decrypt(key.getBase64decodedKey(), bytesToDecrypt);
                        Log.d(TAG, "message decrypted with " + key.getName());
                        ret = GotennaDropDownReceiver.deserializeCotMessageFromGoTenna(decryptedMsg);
                        if(ret != null) {
                            Log.d(TAG, "decrypted msg: " + new String(decryptedMsg));
                            return ret;
                        } else {
                            Log.w(TAG, "SUCCESSFUL DECRYPTION BUT COULD NOT DESERIALIZE");
                        }
                    } catch (Exception decryptException) {
                        Log.d(TAG, "message failed to decrypt with " + key.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unexpected exception decrypting/deserializing goTenna message", e);
        }
        return null;
    }

    public static String decryptChatMsg(String msgToDecrypt) {
        String ret = msgToDecrypt;
        try {
            populateDecryptionKeyList();
            synchronized (decryptionKeyList) {
                byte[] decryptedMsg = null;
                for (Key key : decryptionKeyList) {
                    try {
                        decryptedMsg = EncryptionUtils.decrypt(key.getBase64decodedKey(), Base64.decode(msgToDecrypt, Base64.DEFAULT));
                        if(decryptedMsg != null) {
                            Log.d(TAG, "chat message decrypted with " + key.getName());
                            ret = new String(decryptedMsg);
                            Log.d(TAG, "chat decrypted msg: " + ret);
                            // TODO: check to see if it contains CHAT_DELIM???
                            if(ret.contains(GotennaDropDownReceiver.CHAT_DELIM)) {
                                return ret;
                            } else {
                                return null;
                            }
                        }
                    } catch (Exception decryptException) {
                        Log.w(TAG, "chat message failed to decrypt with " + key.getName(), decryptException);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unexpected exception decrypting/deserializing goTenna chat message", e);
        }
        if(ret.contains(GotennaDropDownReceiver.CHAT_DELIM)) {
            return ret;
        } else {
            return null;
        }
    }

    public static void archiveKey(String mostRecentlySelectedKey) {
        File from = new File(KEY_LOCATION_ON_SDCARD + "/" + mostRecentlySelectedKey);
        File oldKeyDir = new File(OLD_KEY_LOCATION_ON_SDCARD);
        oldKeyDir.mkdirs();
        File to = new File(OLD_KEY_LOCATION_ON_SDCARD + "/" + mostRecentlySelectedKey);
        from.renameTo(to);
    }
}
