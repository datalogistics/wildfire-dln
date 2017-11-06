
package com.atakmap.android.QuickChat.history;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Scott Auman on 5/21/2016.
 */
public class Message implements Parcelable {

    private String from;
    private String to;
    private String message;
    private String date;
    private String time;
    private Long messageDateObj;
    private TYPE type;
    private String uid;

    public enum TYPE {
        SENT,RECEIVED
    }

    public Message() {

    }

    protected Message(Parcel in) {
        from = in.readString();
        message = in.readString();
        date = in.readString();
        time = in.readString();
        messageDateObj = in.readByte() == 0x00 ? null : in.readLong();
        type = (TYPE) in.readValue(TYPE.class.getClassLoader());
        uid = in.readString();
        to = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(from);
        dest.writeString(message);
        dest.writeString(date);
        dest.writeString(time);
        if (messageDateObj == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(messageDateObj);
        }
        dest.writeValue(type);
        dest.writeString(uid);
        dest.writeString(to);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Long getMessageDateObj() {
        return messageDateObj;
    }

    public void setMessageDateObj(Long messageDateObj) {
        this.messageDateObj = messageDateObj;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }
}
