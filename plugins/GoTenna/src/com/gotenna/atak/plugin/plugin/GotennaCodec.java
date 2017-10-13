package com.gotenna.atak.plugin.plugin;

import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MultiPolyline;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Rectangle;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by kusbeck on 2/2/17.
 */

public class GotennaCodec {

    public static final String TAG = GotennaCodec.class.getName();

    public static final int GT_MSG_POINT_MAP_ITEM_V1 = 18001;
    public static final int GT_MSG_POLYLINE_V1 = 18002;
    public static final int GT_MSG_CIRCLE_MAP_ITEM_V1 = 18003;
    public static final int GT_MSG_ELLIPSE_MAP_ITEM_V1 = 18004;
    public static final int GT_MSG_RECTANGLE_V1 = 18005;

    public static byte[] serializeForGoTenna(MapEvent mapEvent) {
        MapItem item = mapEvent.getItem();
        byte[] mapEventBytes = null;

        if (item instanceof PointMapItem) {
            //mapEventBytes = serializePointMapItemV1((PointMapItem) item);
            mapEventBytes = serializePointMapItemV0((PointMapItem) item);
        } else if (item instanceof DrawingRectangle) {
            //mapEventBytes = serializeDrawingRectangleV1((DrawingRectangle) item);
            Log.d(TAG, "Have not implemented sharing of item type: " + item.getClass().getName());
        } else if (item instanceof DrawingShape) {
            //mapEventBytes = serializePolylineV1((DrawingShape) item);
            Log.d(TAG, "Have not implemented sharing of item type: " + item.getClass().getName());
        } else if (item instanceof MultiPolyline) {
            //Unlikely to fit, but TODO
            Log.d(TAG, "Have not implemented sharing of item type: " + item.getClass().getName());
        } else {
            Log.d(TAG, "Could not serialize item of type: " + item.getClass().getName());
        }

        Log.d(TAG, "GoTenna message bytes: " + bytesToHex(mapEventBytes));

        return mapEventBytes;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    private static void writeString(DataOutputStream dos, String str) throws IOException {
        dos.writeShort(str.length());
        dos.writeChars(str);
    }

    private static String readString(DataInputStream dis) throws IOException {
        int strLength = dis.readShort();
        String ret = "";
        for(int i=0; i<strLength; i++) {
            ret += dis.readChar();
        }
        return ret;
    }

    public static byte[] serializePolylineV1(DrawingShape shape) {
        String uid = shape.getUID();
        String callsign = shape.getMetaString("callsign", shape.getMetaString("shapeName", null));
        GeoPoint[] points = shape.getPoints();
        int style = shape.getStyle();
        int lineStyle = shape.getLineStyle();
        int fillColor = shape.getFillColor();
        int strokeColor = shape.getStrokeColor();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeShort(GT_MSG_POLYLINE_V1);
            writeString(dos, uid);
            writeString(dos, callsign);
            dos.writeInt(style);
            dos.writeInt(lineStyle);
            dos.writeInt(fillColor);
            dos.writeInt(strokeColor);
            dos.writeShort(points.length);
            for(GeoPoint point : points) {
                dos.writeDouble(point.getLatitude());
                dos.writeDouble(point.getLongitude());
                dos.writeFloat((float) point.getAltitude().getValue());
            }
        } catch (IOException ioe) {
            Log.d(TAG, "Problem writing GT shape", ioe);
        }

        byte[] clearTextMsg = bos.toByteArray();
        Log.d(TAG, "serialized shape " + callsign + " to " + clearTextMsg.length);
        return clearTextMsg;
    }

    public static CotEvent parsePolylineV1(DataInputStream dis) throws IOException {
        String uid = readString(dis);
        String callsign = readString(dis);
        int style = dis.readInt();
        int lineStyle = dis.readInt();
        int fillColor = dis.readInt();
        int strokeColor = dis.readInt();
        int numPoints = dis.readShort();
        GeoPoint[] points = new GeoPoint[numPoints];
        for(int i=0; i<numPoints; i++) {
            Double lat = dis.readDouble();
            Double lon = dis.readDouble();
            Double alt = (double) dis.readFloat();
            points[i] = new GeoPoint(lat, lon, alt);
        }

        Polyline polyline = new Polyline(uid);
        polyline.setStyle(style);  // shape defaults to closed?
        polyline.setBasicLineStyle(lineStyle);
        polyline.setFillColor(fillColor);
        polyline.setStrokeColor(strokeColor);
        polyline.setMetaString("shapeName", callsign);
        polyline.setPoints(points);

        CotEvent ret = CotEventFactory.createCotEvent(polyline);
        return ret;
    }

    public static byte[] serializeDrawingRectangleV1(DrawingRectangle rectangle) {
        String uid = rectangle.getUID();
        String callsign = rectangle.getMetaString("callsign", rectangle.getMetaString("shapeName", null));
        GeoPoint[] points = rectangle.getPoints();
        int style = rectangle.getStyle();
        int lineStyle = rectangle.getLineStyle();
        int fillColor = rectangle.getFillColor();
        int strokeColor = rectangle.getStrokeColor();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeShort(GT_MSG_RECTANGLE_V1);
            writeString(dos, uid);
            writeString(dos, callsign);
            dos.writeInt(style);
            dos.writeInt(lineStyle);
            dos.writeInt(fillColor);
            dos.writeInt(strokeColor);
            dos.writeShort(points.length);
            for(GeoPoint point : points) {
                dos.writeDouble(point.getLatitude());
                dos.writeDouble(point.getLongitude());
                dos.writeFloat((float) point.getAltitude().getValue());
            }
        } catch (IOException ioe) {
            Log.d(TAG, "Problem writing GT rectangle", ioe);
        }

        byte[] clearTextMsg = bos.toByteArray();
        Log.d(TAG, "serialized rectangle " + callsign + " to " + clearTextMsg.length);
        return clearTextMsg;
    }

    public static CotEvent parseDrawingRectangleV1(DataInputStream dis) throws IOException {
        String uid = readString(dis);
        String callsign = readString(dis);
        int style = dis.readInt();
        int lineStyle = dis.readInt();
        int fillColor = dis.readInt();
        int strokeColor = dis.readInt();
        int numPoints = dis.readShort();
        GeoPoint[] points = new GeoPoint[numPoints];
        for(int i=0; i<numPoints; i++) {
            Double lat = dis.readDouble();
            Double lon = dis.readDouble();
            Double alt = (double) dis.readFloat();
            points[i] = new GeoPoint(lat, lon, alt);
        }

        Rectangle polyline = new Rectangle(uid);
        polyline.setStyle(style);
        polyline.setBasicLineStyle(lineStyle);
        polyline.setFillColor(fillColor);
        polyline.setStrokeColor(strokeColor);
        polyline.setMetaString("shapeName", callsign);
        polyline.setPoints(points);

        CotEvent ret = CotEventFactory.createCotEvent(polyline);
        return ret;
    }

    public static byte[] serializePointMapItemV0(PointMapItem pointMapItem) {
        String DELIM=";";
        String uid = pointMapItem.getUID();
        String how = pointMapItem.getMetaString("how", "h-e");
        String type = pointMapItem.getType();
        if(type.equals("self")) type = "a-f-G-U-C"; // to correct for self points
        String callsign = pointMapItem.getMetaString("callsign", null);
        Double lat = pointMapItem.getPoint().getLatitude();
        Double lon = pointMapItem.getPoint().getLongitude();
        Double alt = pointMapItem.getPoint().getAltitude().getValue();
        String team = pointMapItem.getMetaString("team", "???");
        String clearTextMsg = StringUtils.join(new Object[] {uid, type, callsign, how, lat, lon, alt, team}, DELIM);
        byte[] bytesToSend = clearTextMsg.getBytes();
        Log.d(TAG, "serialized Point (V0) " + callsign + " to " + bytesToSend.length);
        return bytesToSend;
    }

    public static CotEvent parsePointMapItemV0(DataInputStream dis) {
        String DELIM=";";
        try {
            String fromBytes = dis.readLine();
            String[] parts = fromBytes.split(DELIM);
            String uid = parts[0];
            String type = parts[1];
            String callsign = parts[2];
            String how = parts[3];
            double lat = Double.parseDouble(parts[4]);
            double lon = Double.parseDouble(parts[5]);
            double alt = Double.parseDouble(parts[6]);
            String team = null;
            team = parts[7].equals("???") ? null : parts[7];
            CotEvent gtCotEvent = new CotEvent();
            gtCotEvent.setUID(uid);
            gtCotEvent.setType(type);
            CoordinatedTime time = new CoordinatedTime();
            gtCotEvent.setTime(time);
            gtCotEvent.setStart(time);
            gtCotEvent.setStale(time.addMinutes(10));
            gtCotEvent.setHow(how);
            gtCotEvent.setPoint(new CotPoint(lat, lon, alt, CotPoint.COT_CE90_UNKNOWN, CotPoint.COT_LE90_UNKNOWN));
            CotDetail taskDetail = new CotDetail("detail");
            CotDetail contactDetail = new CotDetail("contact");
            contactDetail.setAttribute("callsign", callsign);
            taskDetail.addChild(contactDetail);
            CotDetail summaryDetail = new CotDetail("summary");
            summaryDetail.setInnerText("goTenna");
            taskDetail.addChild(summaryDetail);
            if(team != null) {
                CotDetail uidDetail = new CotDetail("uid");
                uidDetail.setAttribute("Droid", callsign);
                taskDetail.addChild(uidDetail);
                CotDetail groupDetail = new CotDetail("__group");
                groupDetail.setAttribute("name", team);
                taskDetail.addChild(groupDetail);
            }
            gtCotEvent.setDetail(taskDetail);
            return gtCotEvent;
        } catch (Exception e) {
            //TODO!!
        }
        return null;
    }

    public static byte[] serializePointMapItemV1(PointMapItem pointMapItem) {
        String uid = pointMapItem.getUID();
        String how = pointMapItem.getMetaString("how", "h-e");
        String type = pointMapItem.getType();
        if(type.equals("self")) type = "a-f-G-U-C"; // to correct for self points
        String callsign = pointMapItem.getMetaString("callsign", pointMapItem.getMetaString("shapeName", null));
        Double lat = pointMapItem.getPoint().getLatitude();
        Double lon = pointMapItem.getPoint().getLongitude();
        Double alt = pointMapItem.getPoint().getAltitude().getValue();
        String team = pointMapItem.getMetaString("team", "???");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeShort(GT_MSG_POINT_MAP_ITEM_V1);
            writeString(dos, uid);
            writeString(dos, how);
            writeString(dos, type);
            writeString(dos, callsign);
            dos.writeDouble(lat);
            dos.writeDouble(lon);
            dos.writeDouble(alt);
            writeString(dos, team);
        } catch (IOException ioe) {
            Log.w(TAG, "Problem writing goTenna message", ioe);
        }

        byte[] clearTextMsg = bos.toByteArray();
        Log.d(TAG, "serialized point to " + clearTextMsg.length);
        return clearTextMsg;
    }

    public static CotEvent parsePointMapItemV1(DataInputStream dis) throws IOException {
        String uid = readString(dis);
        Log.d(TAG, "uid="+uid);
        String how = readString(dis);
        Log.d(TAG, "how="+how);
        String type = readString(dis);
        Log.d(TAG, "type="+type);
        String callsign = readString(dis);
        Log.d(TAG, "callsign="+callsign);
        Double lat = dis.readDouble();
        Log.d(TAG, "lat="+lat);
        Double lon = dis.readDouble();
        Log.d(TAG, "lon="+lon);
        Double alt = dis.readDouble();
        Log.d(TAG, "alt="+alt);
        String team = readString(dis);
        Log.d(TAG, "team="+team);

        CotEvent gtCotEvent = new CotEvent();
        gtCotEvent.setUID(uid);
        gtCotEvent.setType(type);
        CoordinatedTime time = new CoordinatedTime();
        gtCotEvent.setTime(time);
        gtCotEvent.setStart(time);
        gtCotEvent.setStale(time.addMinutes(10));
        gtCotEvent.setHow(how);
        gtCotEvent.setPoint(new CotPoint(lat, lon, alt, CotPoint.COT_CE90_UNKNOWN, CotPoint.COT_LE90_UNKNOWN));
        CotDetail taskDetail = new CotDetail("detail");
        CotDetail contactDetail = new CotDetail("contact");
        contactDetail.setAttribute("callsign", callsign);
        taskDetail.addChild(contactDetail);
        CotDetail summaryDetail = new CotDetail("summary");
        summaryDetail.setInnerText("goTenna");
        taskDetail.addChild(summaryDetail);
        if(team != null) {
            CotDetail uidDetail = new CotDetail("uid");
            uidDetail.setAttribute("Droid", callsign);
            taskDetail.addChild(uidDetail);
            CotDetail groupDetail = new CotDetail("__group");
            groupDetail.setAttribute("name", team);
            taskDetail.addChild(groupDetail);
        }
        gtCotEvent.setDetail(taskDetail);

        return gtCotEvent;
    }

}
