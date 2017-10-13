package com.atakmap.android.takchat.uid;

import com.atakmap.android.takchat.TAKChatUtils;

import org.jivesoftware.smack.filter.StanzaExtensionFilter;

/**
 *  Stanza filter to identify messages from other TAK users
 *
 *  Created by byoung on 7/31/2016.
 */
public class TAKUidFilter extends StanzaExtensionFilter {

    public TAKUidFilter(){
        super(TAKChatUtils.TAK_XMPP_ELEMENT, TAKChatUtils.TAK_XMPP_NAMESPACE);
    }
}
