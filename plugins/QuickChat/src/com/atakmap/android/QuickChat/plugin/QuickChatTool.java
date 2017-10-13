package com.atakmap.android.QuickChat.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.ViewGroup;

import com.atakmap.android.QuickChat.utils.PluginHelper;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.log.Log;

import transapps.mapi.MapView;
import transapps.maps.plugin.tool.Group;
import transapps.maps.plugin.tool.Tool;
import transapps.maps.plugin.tool.ToolDescriptor;

public class QuickChatTool extends Tool implements ToolDescriptor {

    private final Context context;
    private final String TAG = getClass().getSimpleName();

    public QuickChatTool(Context context) {
        this.context = context;
    }

    @Override
    public String getDescription() {
        return "Quick Chat";
    }

    @Override
    public Drawable getIcon() {
        return (context == null) ? null : context.getResources().getDrawable(
                R.drawable.chatmessageplugin72);
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {
            Group.GENERAL
        };
    }

    @Override
    public String getShortDescription() {
        return "Quick Chat";
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

        Log.i(TAG, "Quick Chat Tool Widget Clicked");
        showChooseDialog().show();
    }

    /**Builds a android dialog that
     * displays 2 buttons, allows user to select to navi to filter list / message history
     * @return Dialog with attri built upon
     */
    private Dialog showChooseDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(
                com.atakmap.android.maps.MapView.getMapView().getContext());
        builder.setMessage(Html
                .fromHtml("<b> User List: </b> \t Display's The Current Users Which Show Popup Messages When Receiving Chat Messages<br> <br>"
                        +
                        "<b>Message History:</font></b> \t Display's All Popup Messages Received From Users"));
        builder.setTitle("Quick Chat Options");
        builder.setIcon(PluginHelper.pluginContext.getResources().getDrawable(
                R.drawable.chatmessageplugin48));
        builder.setCancelable(true)
                .setNegativeButton(Html.fromHtml("User's List"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                AtakBroadcast
                                        .getInstance()
                                        .sendBroadcast(
                                                new Intent(
                                                        "com.atakmap.android.FILTER_USERS_POPUPS"));
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(Html.fromHtml("Message(s) History"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                AtakBroadcast
                                        .getInstance()
                                        .sendBroadcast(
                                                new Intent(
                                                        "com.atakmap.android.QuickChat.SHOW_HISTORY_DROPDOWN"));
                                dialog.dismiss();
                            }
                        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onDeactivate(ToolCallback arg0) {
        Log.d(TAG,"QuickChat tool deactivated");

    }

}
