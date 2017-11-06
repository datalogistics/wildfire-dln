
package com.atakmap.android.wxreport.plugin;

import com.atakmap.android.wxreport.plugin.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;
import transapps.mapi.MapView;
import transapps.maps.plugin.tool.Group;
import transapps.maps.plugin.tool.Tool;
import transapps.maps.plugin.tool.ToolDescriptor;

public class WxReportTool extends Tool implements ToolDescriptor {

    private Context context;

    public WxReportTool(Context context) {
        this.context = context;
    }

    @Override
    public String getDescription() {
        return "Wx Report";
    }

    @Override
    public Drawable getIcon() {
        return (context == null) ? null : context.getResources().getDrawable(
                R.drawable.weatherreportplugin72);
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {
            Group.GENERAL
        };
    }

    @Override
    public String getShortDescription() {
        return "Wx Report";
    }

    @Override
    public Tool getTool() {
        return this;
    }

    @Override
    public void onActivate(Activity arg0, MapView arg1, ViewGroup arg2,
            Bundle arg3,
            ToolCallback arg4) {

        // Hack to close the dropdown that automatically opens when a tool
        // plugin is activated.
        if (arg4 != null) {
            arg4.onToolDeactivated(this);
        }

        // Intent to launch the dropdown or tool

        Intent i = new Intent("com.atakmap.android.wxreport.SHOW_WX_REPORT");
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(i);

    }

    @Override
    public void onDeactivate(ToolCallback arg0) {
    }

}
