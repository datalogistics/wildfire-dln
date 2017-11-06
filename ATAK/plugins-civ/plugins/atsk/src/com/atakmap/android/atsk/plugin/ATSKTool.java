
package com.atakmap.android.atsk.plugin;

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

public class ATSKTool extends Tool implements ToolDescriptor {

    private final Context _context;

    public ATSKTool(Context context) {
        _context = context;
    }

    @Override
    public String getDescription() {
        return "ATSK";
    }

    @Override
    public Drawable getIcon() {
        return (_context == null) ? null :
                _context.getResources().getDrawable(
                        R.drawable.ic_menu_atsk);
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {
                Group.GENERAL
        };
    }

    @Override
    public String getShortDescription() {
        return "ATSK Tool";
    }

    @Override
    public Tool getTool() {
        return this;
    }

    @Override
    public void onActivate(Activity arg0, MapView arg1, ViewGroup arg2,
            Bundle arg3,
            ToolCallback arg4) {

        //         <ActionMenu title="ATSK" iconPath="ic_menu_atsk" preferredMenu="actionBar" hideable="false">
        //            <broadcast>
        //                <action>com.gmeci.atsk.ACTION_BAR</action>
        //            </broadcast>
        //        </ActionMenu>

        // Intent to launch the dropdown or tool

        //arg2.setVisibility(ViewGroup.INVISIBLE);
        Intent i = new Intent("com.gmeci.atsk.ACTION_BAR");
        com.atakmap.android.ipc.AtakBroadcast.getInstance().sendBroadcast(i);

    }

    @Override
    public void onDeactivate(ToolCallback arg0) {
    }

}
