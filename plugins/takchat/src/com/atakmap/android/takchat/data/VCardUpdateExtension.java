package com.atakmap.android.takchat.data;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * XMPP Extension to send VCard update (e.g. photo hash) in <code>{@link org.jivesoftware.smack.packet.Presence}</code> message
 *
 * Created by byoung on 9/28/2016.
 */
public class VCardUpdateExtension implements ExtensionElement {

    public static final String NAMESPACE = "vcard-temp:x:update";
    public static final String ELEMENT_NAME = "x";

    public static final String PHOTO_NAME = "photo";

    private String photoHash;

    public VCardUpdateExtension() {
        photoHash = null;
    }

    public boolean isEmpty() {
        return FileSystemUtils.isEmpty(photoHash);
    }

    public String getPhotoHash() {
        return photoHash;
    }

    public void setPhotoHash(String hash) {
        this.photoHash = hash;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement(PHOTO_NAME, this.photoHash);
        xml.closeElement(this);
        return xml;
    }
}
