package com.atakmap.android.takchat.data;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse linked content from strings e.g. locations
 *
 * Created by byoung on 11/1/16.
 */

public class MessageLocationLink {
    private static final String TAG = "MessageLocationLink";

    /**
     * Enumerate supported location strings
     *
     *  TODO support Lat Long N/S, E/W, and no decimals?
     *      allow relative e.g. 400m N of me, or 1000ft south of you?
     *      what other formats do we need?
     */
    public enum LocationLinkType{
        MGRS(Pattern.compile("\\d{1,2}[A-Za-z]\\s*[A-Za-z]{2}\\s*\\d{1,5}\\s*\\d{1,5}")),
        LATLON(Pattern.compile(
                "(-?([1-8]?[1-9]|[1-9]0|0)\\.{1}\\d{1,6})"
                        + "(\\s*|,|,\\s*)"
                        + "(-?([1]?[1-7][1-9]|[1]?[1-8][0]|[1-9]?[0-9]|10[0-9])\\.{1}\\d{1,6})"));

        private Pattern pattern;

        private LocationLinkType(Pattern p){
            this.pattern = p;
        }

        public Pattern getPattern(){
            return this.pattern;
        }
    };

    private int startIndex;
    private LocationLinkType type;
    private String linkText;

    public MessageLocationLink(int startIndex, LocationLinkType type, String linkText) {
        this.startIndex = startIndex;
        this.type = type;
        this.linkText = linkText;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public LocationLinkType getType() {
        return type;
    }

    public String getLinkText() {
        return linkText;
    }

    public boolean isValid(){
        if(FileSystemUtils.isEmpty(linkText))
            return false;

        if(startIndex < 0)  //|| startIndex < fullMessageBody.length() - MIN_LENGTH)
            return false;

        return true;
    }

    @Override
    public String toString() {
        return type + " (" + startIndex + ") " + linkText;
    }

    /**
     * Parse list of locations in the specified message
     *
     * @param message
     * @return map of starting index to full location string
     */
    public static List<MessageLocationLink> getLocations(String message) {
        List<MessageLocationLink> locations = new ArrayList<MessageLocationLink>();
        if(FileSystemUtils.isEmpty(message)){
            return locations;
        }

        //run all patterns to identify locations
        for(LocationLinkType type : LocationLinkType.values()){
            Matcher matcher = type.getPattern().matcher(message);
            while (matcher.find()) {
                MessageLocationLink location = new MessageLocationLink(matcher.start(), type, matcher.group());
                Log.d(TAG, type.name() + " found match: " + locations.toString());
                locations.add(location);
            }
        }

        if(locations.size() > 0)
            Log.d(TAG, "Found location count: " + locations.size());

        return locations;
    }

}
