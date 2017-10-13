
package com.gmeci.atsk.resources;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Base64;
import android.util.Xml;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Load menu and base64 encode plugin-only assets
 */
public class ATSKMenuLoader {

    public static String TAG = "ATSKMenuLoader";

    private static HashMap<String, String> _menuCache = new HashMap<String, String>();
    private static HashMap<String, String> _itemCache = new HashMap<String, String>();

    // Load and encode menu or return cached result
    public static String loadMenu(Context ctx, String menuPath, boolean xmlOnly) {
        if (_menuCache.containsKey(menuPath))
            return _menuCache.get(menuPath);

        Log.d(TAG, "Loading ATSK menu " + menuPath);

        InputStream is = null;
        AssetManager assets = ctx.getAssets();
        try {
            // Copy XML to string for modification during XML reading
            is = assets.open(menuPath);
            String xmlContent = new String(FileSystemUtils.read(is),
                    FileSystemUtils.UTF8_CHARSET);
            is.close();

            // In case we only want the unmodified XML content
            if (xmlOnly) {
                _menuCache.put(menuPath, xmlContent);
                return xmlContent;
            }

            // Re-open stream
            is = assets.open(menuPath);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setFeature(Xml.FEATURE_RELAXED, true);
            parser.setInput(is, null);

            int eventType;
            do {
                eventType = parser.next();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String tag = parser.getName();
                        if (tag.equals("button")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attr = parser.getAttributeName(i);
                                String value = parser.getAttributeValue(i);

                                if (attr.equals("icon")
                                        || attr.equals("onClick")) {
                                    // Replace with base64 encoded content
                                    String b64 = getBase64Item(value);
                                    if (!b64.isEmpty())
                                        xmlContent = xmlContent.replace(value,
                                                b64);
                                } else if (attr.equals("submenu")) {
                                    String subMenu = loadMenu(ctx, value,
                                            xmlOnly);
                                    if (!subMenu.equals(value)) {
                                        // TODO: ATAK support to load base64 encoded menu
                                        // see MenuLayoutWidget.load()

                                        /*String b64 = "base64:/" + new String(
                                                Base64.encode(subMenu.getBytes(),
                                                Base64.URL_SAFE | Base64.NO_WRAP));
                                        xmlContent = xmlContent.replace(value, b64);*/

                                        // Send it through without base64 for now
                                        subMenu = subMenu.replaceAll("'", "\"")
                                                .replaceAll("\t", " ")
                                                .replaceAll("\n", "")
                                                .replaceAll("<", "&lt;")
                                                .replaceAll(">", "&gt;");
                                        xmlContent = xmlContent.replace(value,
                                                subMenu);
                                    }
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        break;
                    default:
                        break;
                }
            } while (eventType != XmlPullParser.END_DOCUMENT);
            _menuCache.put(menuPath, xmlContent);
            return xmlContent;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load ATSK menu asset: " + menuPath
                    + " (" + e.getMessage() + ")");
            return menuPath;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                Log.d(TAG, "error closing menu asset: " + menuPath);
            }
        }
    }

    public static String loadMenu(String menuPath) {
        return loadMenu(ATSKApplication.getInstance().
                getPluginContext(), menuPath, false);
    }

    public static String getBase64Item(final String file) {
        InputStream is = null;
        ByteArrayOutputStream outputStream = null;

        if (_itemCache.containsKey(file))
            return _itemCache.get(file);

        try {
            is = ATSKApplication.getInstance().
                    getPluginContext().getAssets().open(file);
            outputStream = new ByteArrayOutputStream();
            int size;
            byte[] buffer = new byte[1024];

            while ((size = is.read(buffer, 0, 1024)) >= 0) {
                outputStream.write(buffer, 0, size);
            }
            buffer = outputStream.toByteArray();

            String data = "base64:/" + new String(Base64.encode(buffer,
                    Base64.URL_SAFE | Base64.NO_WRAP),
                    FileSystemUtils.UTF8_CHARSET);

            _itemCache.put(file, data);
            return data;
        } catch (Exception e) {
            //Log.e(TAG, "Failed to load item " + file, e);
            return "";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
