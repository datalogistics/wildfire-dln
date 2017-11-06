
package com.atakmap.android.takchat.view.badge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.takchat.plugin.R;

import java.util.HashMap;

/**
 * Copy of same class in ATAK project due to context issue
 * Extended with Text Size and connectivity layer
 */
public class AtakLayerDrawableUtil extends BroadcastReceiver {

    private static AtakLayerDrawableUtil instance;
    private final HashMap<String, String> atakLayerBroadcastMap;
    private Context context;
    private final DocumentedIntentFilter inf;

    synchronized public static AtakLayerDrawableUtil getInstance(Context context) {
        if (instance == null) {
            instance = new AtakLayerDrawableUtil(context);
        } else {
            instance.context = context;
        }
        return instance;
    }

    private AtakLayerDrawableUtil(Context context) {
        this.context = context;
        atakLayerBroadcastMap = new HashMap<String, String>();
        inf = new DocumentedIntentFilter();
        AtakBroadcast.getInstance().registerReceiver(this, inf);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (atakLayerBroadcastMap != null) {
            String badgeString = intent.getStringExtra("badge_string");
            if (badgeString != null) {
                atakLayerBroadcastMap.put(intent.getAction(), badgeString);
            }
        }
    }

    public void setBadgeString(String iconPath, String badgeString) {
        if (iconPath != null && !iconPath.isEmpty() && badgeString != null) {
            atakLayerBroadcastMap.put(iconPath, badgeString);
        }
    }

    public String getBadgeString(String iconPath) {
        String ret = "";
        if (atakLayerBroadcastMap != null) {
            ret = atakLayerBroadcastMap.get(iconPath);
        }
        return ret;
    }

    public int getBadgeInt(String iconPath) {
        int ret = 0;
        if (atakLayerBroadcastMap != null) {
            String str = atakLayerBroadcastMap.get(iconPath);
            if (str != null && !str.isEmpty()) {
                ret = Integer.parseInt(str);
            }

        }
        return ret;
    }

    synchronized public void addLayerDrawableBroadcastString(
            String broadcastString) {
        if (!inf.hasAction(broadcastString)) {
            AtakBroadcast.getInstance().unregisterReceiver(this);
            inf.addAction(broadcastString);
            AtakBroadcast.getInstance().registerReceiver(this, inf);
        }
    }

    public void dispose() {
        try {
            AtakBroadcast.getInstance().unregisterReceiver(this);
        } catch (Exception e) {
            // no need to actually print the error, already disposed
        }

    }

    /**
     * Note, the Drawable layer-list must have a layer with id = "ic_badge"
     * @param icon
     * @param count
     */
    public void setBadgeCount(LayerDrawable icon, int count) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }

//    public void setBadgeCount(LayerDrawable icon, int baseResource, int count) {
//        BadgeDrawable badge;
//
//        // Reuse drawable if possible
//        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
//        if (reuse != null && reuse instanceof BadgeDrawable) {
//            badge = (BadgeDrawable) reuse;
//        } else {
//            badge = new BadgeDrawable(context);
//        }
//
//        badge.setCount(count);
//        icon.mutate();
//        icon.setDrawableByLayerId(R.id.ic_badge, badge);
//
//
//        //TODO reuse drawable for efficiency?
//        if(baseResource != -1) {
//            Drawable base = TAKChatUtils.pluginContext.getResources().getDrawable(baseResource);
//            if (base != null)
//                icon.setDrawableByLayerId(R.id.attachment_base, base);
//        }
//    }

    public void setBadgeCount(LayerDrawable icon, Drawable baseDrawable, int count, float textSize) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count, textSize);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);

        if(baseDrawable != null)
            icon.setDrawableByLayerId(R.id.attachment_base, baseDrawable);
    }

    public void setBadgeCount(LayerDrawable icon, Drawable baseDrawable, int count, boolean bConnected) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count, bConnected);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);

        if(baseDrawable != null)
            icon.setDrawableByLayerId(R.id.attachment_base, baseDrawable);
    }

    public void setBadgeCount(LayerDrawable icon, Drawable baseDrawable, int count) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);

        if(baseDrawable != null)
            icon.setDrawableByLayerId(R.id.attachment_base, baseDrawable);
    }

    public static void setBadgeCount(Context context, LayerDrawable icon,
            int count) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }

}
