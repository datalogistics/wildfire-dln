package com.atakmap.android.takchat.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.android.contact.AvatarFeature;
import com.atakmap.android.contact.XmppConnector;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

/**
 * An Avatar, backed by an XMPP VCard
 *
 * Created by byoung on 1/13/17.
 */
public class VCardAvatarFeature extends AvatarFeature {

    private final VCard _card;

    public VCardAvatarFeature(VCard card) {
        _card = card;
    }

    public boolean isValid(){
        return _card != null;
    }

    @Override
    public Bitmap getAvatar() {
        if(!isValid())
            return null;

        byte[] avatar = _card.getAvatar();
        if(FileSystemUtils.isEmpty(avatar))
            return null;

        return BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
    }

    @Override
    public byte[] getAvatarBytes() {
        if(!isValid())
            return null;

        return _card.getAvatar();
    }

    @Override
    public String getHash(){
        if(!isValid())
            return null;

        return _card.getAvatarHash();
    }

    @Override
    public String getConnectorType() {
        return XmppConnector.CONNECTOR_TYPE;
    }
}
