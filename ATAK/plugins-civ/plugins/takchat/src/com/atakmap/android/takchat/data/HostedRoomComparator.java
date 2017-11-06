package com.atakmap.android.takchat.data;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.jivesoftware.smackx.muc.HostedRoom;

import java.util.Comparator;

/**
 * Compares Hosted Rooms for sorting
 * See also <code>{@link org.jivesoftware.smackx.muc.HostedRoom}</code>
 *
 * Created by byoung on 10/7/16.
 */
public class HostedRoomComparator implements Comparator<HostedRoom> {


    @Override
    public int compare(HostedRoom lhs, HostedRoom rhs) {
        if(lhs == null)
            return 1;
        else if(rhs == null)
            return -1;

        if(FileSystemUtils.isEmpty(lhs.getName()))
            return 1;
        if(FileSystemUtils.isEmpty(rhs.getName()))
            return -1;

        return lhs.getName().compareTo(rhs.getName());
    }
}
