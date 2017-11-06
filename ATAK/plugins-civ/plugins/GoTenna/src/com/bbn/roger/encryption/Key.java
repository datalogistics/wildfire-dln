package com.bbn.roger.encryption;

import android.util.Base64;

/**
 * Created by kusbeck on 12/20/16.
 */

public class Key {

    private byte[] base64decodedKey;
    private String name;

    public Key(byte[] base64decodedKey) {
        this.base64decodedKey = base64decodedKey;
        this.name = null;
    }

    public Key(byte[] base64decodedKey, String name) {
        this.base64decodedKey = base64decodedKey;
        this.name = name;
    }

    public String getName() { return name; }

    public byte[] getBase64decodedKey() { return base64decodedKey; }

    public byte[] getBase64encodedKey() {
        return Base64.encode(getBase64decodedKey(), Base64.DEFAULT);
    }

    public void setName(String name) {
        this.name = name;
    }
}