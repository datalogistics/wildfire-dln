package com.atakmap.android.takchat.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.contact.AvatarFeature;
import com.atakmap.android.image.ImageActivity;
import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.takchat.TAKChatDropDownReceiver;
import com.atakmap.android.takchat.TAKChatUtils;
import com.atakmap.android.takchat.adapter.ContactListAdapter;
import com.atakmap.android.takchat.data.VCardAvatarFeature;
import com.atakmap.android.takchat.data.VCardListener;
import com.atakmap.android.takchat.data.XmppContact;
import com.atakmap.android.takchat.net.ContactManager;
import com.atakmap.android.takchat.net.TAKChatXMPP;
import com.atakmap.android.takchat.net.VCardManager;
import com.atakmap.android.takchat.plugin.R;
import com.atakmap.android.tools.menu.ActionBroadcastData;
import com.atakmap.android.tools.menu.ActionBroadcastExtraStringData;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;

/**
 * UI for viewing/editing contact info
 *  See <code>{@link VCard}</code>
 *
 * Created by byoung on 9/23/2016.
 */
public class TAKContactProfileView implements VCardListener{

    private static final String TAG = "TAKContactProfileView";
    public static final int IMAGE_SELECT_CODE = 8754;

    private static TAKContactProfileView _instance;

    private XmppContact _displayed;
    private VCard _card;

    private Dialog _dialog;

    public static TAKContactProfileView getInstance() {
        if (_instance == null) {
            _instance = new TAKContactProfileView();
        }
        return _instance;
    }

    private TAKContactProfileView() {
        clear();
    }

    private void clear(){
        if(_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
        _dialog = null;
        _displayed = null;
        _card = null;
    }


    public void showContactInfo(final String bareJid) {
        XmppContact contact = null;
        try{
            contact = TAKChatUtils.takChatComponent.getManager(ContactManager.class).getContactById(JidCreate.bareFrom(bareJid));
        } catch (XmppStringprepException e) {
            Log.e(TAG, "Error getting contact info", e);
            return;
        }

        if(contact == null){
            Log.w(TAG, "Unable to find contact for chat: " + bareJid);
            return;
        }

        showContactInfo(contact);
    }

    /**
     * Show VCard based profile for specified contact
     * And udpate latest freom server
     *
     * @param contact
     */
    public void showContactInfo(final XmppContact contact) {
        showContactInfo(contact, null, VCardManager.UpdateMode.Force);
    }

    /**
     * Show VCard based profile for specified contact
     *
     * @param contact
     * @param provided Use this card, or if null look it up in local DB. Optionally download latest
     *                 from server
     */
    public void showContactInfo(final XmppContact contact, VCard provided, VCardManager.UpdateMode mode) {
        if(contact == null){
            Log.w(TAG, "Cannot show info for invalid contact");
            Toast.makeText(TAKChatUtils.mapView.getContext(), "Failed to load contact", Toast.LENGTH_SHORT).show();
            return;
        }

        _displayed = contact;

        boolean bIsSelf = TAKChatUtils.isMe(contact.getId());

        final boolean bConnected = TAKChatXMPP.getInstance().isConnected();
        final boolean bEditable = bIsSelf && bConnected;
        Log.d(TAG, "Showing contact: " + contact.getId().toString() + ", " + bIsSelf + ", " + bEditable);

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contactView = inflater.inflate(R.layout.takchat_contact_info, null);
        builder.setView(contactView)
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle((bIsSelf ? "Your Profile: " : "User Profile: ") + contact.getId())
                //not cancelable so we can "clear" w/out on dismiss listener. We also refresh this
                //dialog if a VCard update is received
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clear();
                    }
                });
        final AlertDialog dlg = builder.create();

        final TextView contact_info_alias = (TextView) contactView.findViewById(R.id.contact_info_alias);
        contact_info_alias.setText(contact.getName());
        final View contact_info_available = contactView.findViewById(R.id.contact_info_available);
        setStatus(contact, contact_info_available);
        contactView.findViewById(R.id.contact_info_alias_layout).setVisibility(bIsSelf ? View.GONE : View.VISIBLE);

        final TextView contact_info_status = (TextView) contactView.findViewById(R.id.contact_info_status);
        String status = contact.getStatus();
        if(FileSystemUtils.isEmpty(status) && !bIsSelf){
            status = TAKChatUtils.getPluginString(R.string.not_available);
        }
        contact_info_status.setText(status);
        final TextView contact_info_jid = (TextView) contactView.findViewById(R.id.contact_info_jid);
        contact_info_jid.setText(contact.getId().toString());

        ImageButton contact_info_aliasEdit = (ImageButton) contactView.findViewById(R.id.contact_info_aliasEdit);
        contact_info_aliasEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Edit Alias");
                final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                input.setText(contact.getName());

                AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                        .setTitle("Enter alias")
                        .setView(input)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String name = input.getText().toString();
                                        TAKChatUtils.takChatComponent.getManager(ContactManager.class)
                                                .setName(contact.getId(), name);
                                        //TODO listen for refresh when the presence update comes back from server
                                        //update this view and any other relevant UIs
                                    }
                                })
                        .setNegativeButton("Cancel", null);
                build.show();
            }
        });
        contact_info_aliasEdit.setEnabled(bConnected);

        ImageButton contact_info_statusEdit = (ImageButton) contactView.findViewById(R.id.contact_info_statusEdit);
        contact_info_statusEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Edit Status");
                TAKContactsView.getStatus(PreferenceManager.getDefaultSharedPreferences(TAKChatUtils.mapView.getContext()));
                //TODO listen for refresh when the presence update comes back from server
                //update this view and any other relevant UIs
            }
        });
        contactView.findViewById(R.id.contact_info_statusEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);


        final ImageView contact_info_takicon = (ImageView) contactView.findViewById(R.id.contact_info_takicon);
        final TextView contact_info_tak_callsign = (TextView) contactView.findViewById(R.id.contact_info_tak_callsign);
        final TextView contact_info_client = (TextView) contactView.findViewById(R.id.contact_info_client);
        contact_info_client.setText(contact.getClientSoftware().toString());

        final Marker pmi = contact.getMarker();
        if(pmi != null){
            Log.d(TAG, "Setting marker for view: " + pmi.getUID()); // TODO remove some logging
            ATAKUtilities.SetIcon(TAKChatUtils.mapView.getContext(), contact_info_takicon, pmi);
            contact_info_tak_callsign.setText(bIsSelf ? TAKChatUtils.mapView.getDeviceCallsign() : pmi.getTitle());
        }else{
            Log.d(TAG, "Not setting marker for view: " + contact.getId().toString()); // TODO remove some logging
            contact_info_takicon.setVisibility(ImageView.GONE);
            contact_info_takicon.setColorFilter(Color.WHITE);
            contact_info_tak_callsign.setVisibility(ImageView.GONE);
        }

        final ImageView avatarView = (ImageView) contactView.findViewById(R.id.contact_info_alias_avatar);
        final ImageView avatarViewOpen = (ImageView) contactView.findViewById(R.id.contact_info_alias_avatarOpen);
        final TextView avatarSize = (TextView) contactView.findViewById(R.id.contact_info_alias_avatarSize);
        final ImageView contact_info_alias_avatarEdit = (ImageView) contactView.findViewById(R.id.contact_info_alias_avatarEdit);
        final ImageView contact_info_alias_avatarDelete = (ImageView) contactView.findViewById(R.id.contact_info_alias_avatarDelete);
        final View avatarLayout = contactView.findViewById(R.id.contact_info_alias_avatar_layout);

        try {
            EntityBareJid jid = JidCreate.entityBareFrom(contact.getId());
            //display current VCard
            //if card not provided, look it up
            //TODO may want to have a setting to a) pull this every time, or b) display a sync button
            _card = (provided != null) ? provided :
                    TAKChatUtils.takChatComponent.getManager(VCardManager.class).getVCard(
                            jid, mode);
            if(_card == null){
                //no vcard locally stored
                Log.d(TAG, "No VCard found");
                contactView.findViewById(R.id.contact_info_vcard_text_layout).setVisibility(View.GONE);
                avatarLayout.setVisibility(View.GONE);
            }else{
                Log.d(TAG, "VCard found for: " + contact.getId().toString());
                //TODO display full VCard, other fields...
                byte[] avatar = _card.getAvatar();
                if(!FileSystemUtils.isEmpty(avatar)) {
                    Log.d(TAG, "Displaying avatar");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
                    avatarView.setImageBitmap(bitmap);

                    avatarView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AvatarFeature.openAvatar(new VCardAvatarFeature(_card), pmi, HashingUtils.md5sum(_card.getFrom().toString()));
                            clear();
                        }
                    });
                    avatarViewOpen.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AvatarFeature.openAvatar(new VCardAvatarFeature(_card), pmi, HashingUtils.md5sum(_card.getFrom().toString()));
                            clear();
                        }
                    });

                    contact_info_alias_avatarDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TAKChatUtils.takChatComponent.getManager(VCardManager.class).clearMyAvatar();
                            clear();
                        }
                    });
                    contactView.findViewById(R.id.contact_info_alias_avatarEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                    contactView.findViewById(R.id.contact_info_alias_avatarDelete).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                    avatarSize.setVisibility(View.VISIBLE);

                    avatarSize.setText(MathUtils.GetLengthString(avatar.length));
                } else if(bIsSelf){
                    contactView.findViewById(R.id.contact_info_alias_avatarEdit).setVisibility(View.VISIBLE);
                    contactView.findViewById(R.id.contact_info_alias_avatarOpen).setVisibility(View.GONE);
                    contactView.findViewById(R.id.contact_info_alias_avatarDelete).setVisibility(View.GONE);
                    avatarSize.setVisibility(View.GONE);
                } else{
                    contactView.findViewById(R.id.contact_info_alias_avatarEdit).setVisibility(View.GONE);
                    contactView.findViewById(R.id.contact_info_alias_avatarOpen).setVisibility(View.GONE);
                    contactView.findViewById(R.id.contact_info_alias_avatarDelete).setVisibility(View.GONE);
                    avatarSize.setVisibility(View.GONE);
                }

                contact_info_alias_avatarEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getAvatar();
                        clear();
                    }
                });

                final String fname = getFullNameDisplay(_card);
                final boolean bHideFields = false; //!bEditable;
                setTextView((TextView)contactView.findViewById(R.id.contact_info_fullname),
                        (TextView)contactView.findViewById(R.id.contact_info_fullname_text),
                        fname, bHideFields);
                contactView.findViewById(R.id.contact_info_fullnameEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_fullnameEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getFullName(_card, contact);
                    }
                });


                setTextView((TextView)contactView.findViewById(R.id.contact_info_nickname),
                        (TextView)contactView.findViewById(R.id.contact_info_nickname_text),
                        _card.getNickName(), bHideFields);
                contactView.findViewById(R.id.contact_info_nicknameEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_nicknameEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Edit Nickname");
                        final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                        input.setText(_card.getNickName());

                        AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Enter Nickname")
                                .setView(input)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Note we only edit our own V_card
                                                //TODO null check card
                                                String inputStr = input.getText().toString();
                                                _card.setNickName(inputStr);
                                                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(_card);

                                                //TODO listen for refresh when the presence update comes back from server
                                                //update this view and any other relevant UIs
                                            }
                                        })
                                .setNegativeButton("Cancel", null);
                        build.show();
                    }
                });


                setTextView((TextView)contactView.findViewById(R.id.contact_info_org),
                        (TextView)contactView.findViewById(R.id.contact_info_org_text),
                        _card.getOrganization(), bHideFields);
                contactView.findViewById(R.id.contact_info_orgEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_orgEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Edit Organization");
                        final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                        input.setText(_card.getOrganization());

                        AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Enter Organization")
                                .setView(input)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Note we only edit our own VCard
                                                //TODO null check _card
                                                String inputStr = input.getText().toString();
                                                _card.setOrganization(inputStr);
                                                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(_card);

                                                //TODO listen for refresh when the presence update comes back from server
                                                //update this view and any other relevant UIs
                                            }
                                        })
                                .setNegativeButton("Cancel", null);
                        build.show();
                    }
                });

                setTextView((TextView)contactView.findViewById(R.id.contact_info_orgUnit),
                        (TextView)contactView.findViewById(R.id.contact_info_orgUnit_text),
                        _card.getOrganizationUnit(), bHideFields);
                contactView.findViewById(R.id.contact_info_orgUnitEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_orgUnitEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Edit Organization Unit");
                        final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                        input.setText(_card.getOrganizationUnit());

                        AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Enter Organization Unit")
                                .setView(input)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Note we only edit our own VCard
                                                //TODO null check card
                                                String inputStr = input.getText().toString();
                                                _card.setOrganizationUnit(inputStr);
                                                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(_card);

                                                //TODO listen for refresh when the presence update comes back from server
                                                //update this view and any other relevant UIs
                                            }
                                        })
                                .setNegativeButton("Cancel", null);
                        build.show();
                    }
                });

                setTextView((TextView)contactView.findViewById(R.id.contact_info_workEmail),
                        (TextView)contactView.findViewById(R.id.contact_info_workEmail_text),
                        _card.getEmailWork(), bHideFields);
                contactView.findViewById(R.id.contact_info_workEmailEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_workEmailEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Edit Work Email");
                        final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                        input.setText(_card.getEmailWork());
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                        AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Enter Work Email")
                                .setView(input)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Note we only edit our own VCard
                                                //TODO null check card
                                                String inputStr = input.getText().toString();
                                                _card.setEmailWork(inputStr);
                                                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(_card);

                                                //TODO listen for refresh when the presence update comes back from server
                                                //update this view and any other relevant UIs
                                            }
                                        })
                                .setNegativeButton("Cancel", null);
                        build.show();
                    }
                });

                setTextView((TextView)contactView.findViewById(R.id.contact_info_homeEmail),
                        (TextView)contactView.findViewById(R.id.contact_info_homeEmail_text),
                        _card.getEmailHome(), bHideFields);
                contactView.findViewById(R.id.contact_info_homeEmailEdit).setVisibility(bEditable ? View.VISIBLE : View.GONE);
                contactView.findViewById(R.id.contact_info_homeEmailEdit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Edit Home Email");
                        final EditText input = new EditText(TAKChatUtils.mapView.getContext());
                        input.setText(_card.getEmailHome());
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                        AlertDialog.Builder build = new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                                .setTitle("Enter Home Email")
                                .setView(input)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Note we only edit our own VCard
                                                //TODO null check card
                                                String inputStr = input.getText().toString();
                                                _card.setEmailHome(inputStr);
                                                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(_card);

                                                //TODO listen for refresh when the presence update comes back from server
                                                //update this view and any other relevant UIs
                                            }
                                        })
                                .setNegativeButton("Cancel", null);
                        build.show();
                    }
                });

                //TODO finish other vcard text fields
            }
        } catch (XmppStringprepException e) {
            Log.w(TAG, "Invalid VCard jid", e);
        }

        // set dialog dims appropriately based on device size
        WindowManager.LayoutParams screenLP = new WindowManager.LayoutParams();
        screenLP.copyFrom(dlg.getWindow().getAttributes());
        screenLP.width = WindowManager.LayoutParams.MATCH_PARENT;
        screenLP.height = WindowManager.LayoutParams.MATCH_PARENT;
        dlg.getWindow().setAttributes(screenLP);

        _dialog = dlg;
        dlg.show();
    }

    private static void getAvatar() {
        Log.d(TAG, "Edit Avatar");

        final CharSequence[] items = {
                TAKChatUtils.mapView.getContext().getString(com.atakmap.app.R.string.camera),
                TAKChatUtils.mapView.getContext().getString(com.atakmap.app.R.string.image)
        };

        new AlertDialog.Builder(TAKChatUtils.mapView.getContext())
                .setTitle(com.atakmap.app.R.string.attachment_title)
                .setIcon(com.atakmap.app.R.drawable.attachment)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: {
                                //use ImageActivity to launch camera and capture image for attachment
                                String avatarPath = ImageDropDownReceiver
                                        .createAndGetPathToImageFromUID(TAKChatUtils.mapView.getDeviceUid(), "jpg")
                                        .getAbsolutePath();
                                Log.d(TAG, "Starting ImageActivity: " + avatarPath);

                                ArrayList<ActionBroadcastExtraStringData> extras = new ArrayList<ActionBroadcastExtraStringData>();
                                extras.add(new ActionBroadcastExtraStringData(
                                        TAKChatDropDownReceiver.AVATAR_CAPTURED_FILEPATH, avatarPath));
                                ImageActivity imageActivity = new ImageActivity(
                                        TAKChatUtils.mapView.getContext(), TAKChatUtils.mapView.getDeviceUid(),
                                        new ActionBroadcastData(TAKChatDropDownReceiver.AVATAR_CAPTURED, extras),
                                        avatarPath, TAKChatUtils.mapView, false);

                                imageActivity.start();
                            }
                            break;
                            case 1: {//Image
                                dialog.dismiss();
                                try {
                                    Intent agc = new Intent();
                                    agc.setType("image/*");
                                    agc.setAction(Intent.ACTION_GET_CONTENT);
                                    ((Activity) TAKChatUtils.mapView.getContext())
                                            .startActivityForResult(
                                                    agc,
                                                    IMAGE_SELECT_CODE);
                                } catch (Exception e) {
                                    Log.w(TAG,
                                            "Failed to ACTION_GET_CONTENT image",
                                            e);
                                    Toast.makeText(
                                            TAKChatUtils.mapView.getContext(),
                                            com.atakmap.app.R.string.install_gallery,
                                            Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                            break;
                        }
                    }
                }).show();
    }

    private static String getFullNameDisplay(VCard card) {
        //Note we dont use VCard "FN" field b/c Smack only sets First, Middle, Last in FN
        //and our UI does not currently have separate fields for prefix & suffix

        String name = "";
        if(!FileSystemUtils.isEmpty(card.getPrefix()))
            name += card.getPrefix() + " ";
        if(!FileSystemUtils.isEmpty(card.getFirstName()))
            name += card.getFirstName() + " ";
        if(!FileSystemUtils.isEmpty(card.getMiddleName()))
            name += card.getMiddleName() + " ";
        if(!FileSystemUtils.isEmpty(card.getLastName()))
            name += card.getLastName() + " ";
        if(!FileSystemUtils.isEmpty(card.getSuffix()))
            name += card.getSuffix();
        return name;
    }

    private static void getFullName(final VCard card, final XmppContact contact) {
        if(card == null){
            Log.w(TAG, "Cannot edit name for invalid card");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(TAKChatUtils.mapView.getContext());
        LayoutInflater inflater = (LayoutInflater)TAKChatUtils.pluginContext.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contactView = inflater.inflate(R.layout.takchat_contact_name_info, null);
        builder.setView(contactView)
                .setIcon(com.atakmap.app.R.drawable.xmpp_icon)
                .setTitle("User Profile: " + contact.getId().toString());
        final AlertDialog dlg = builder.create();

        final TextView contact_name_prefix = (TextView) contactView.findViewById(R.id.contact_info_name_prefix_text);
        contact_name_prefix.setText(card.getPrefix());

        final TextView contact_name_first = (TextView) contactView.findViewById(R.id.contact_info_name_first_text);
        contact_name_first.setText(card.getFirstName());

        final TextView contact_name_middle = (TextView) contactView.findViewById(R.id.contact_info_name_middle_text);
        contact_name_middle.setText(card.getMiddleName());

        final TextView contact_name_last = (TextView) contactView.findViewById(R.id.contact_info_name_last_text);
        contact_name_last.setText(card.getLastName());

        final TextView contact_name_suffix = (TextView) contactView.findViewById(R.id.contact_info_name_suffix_text);
        contact_name_suffix.setText(card.getSuffix());

        dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Log.d(TAG, "Cancel Set name: " + getFullNameDisplay(card));
            }
        });

        dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                card.setPrefix(contact_name_prefix.getText().toString());
                card.setFirstName(contact_name_first.getText().toString());
                card.setMiddleName(contact_name_middle.getText().toString());
                card.setLastName(contact_name_last.getText().toString());
                card.setSuffix(contact_name_suffix.getText().toString());

                String fullName = getFullNameDisplay(card);
                Log.d(TAG, "Set name: " + fullName);
                TAKChatUtils.takChatComponent.getManager(VCardManager.class).save(card);
                //TODO refresh the base dialog...
            }
        });


        // set dialog dims appropriately based on device size
        WindowManager.LayoutParams screenLP = new WindowManager.LayoutParams();
        screenLP.copyFrom(dlg.getWindow().getAttributes());
        screenLP.width = WindowManager.LayoutParams.MATCH_PARENT;
        screenLP.height = WindowManager.LayoutParams.MATCH_PARENT;
        dlg.getWindow().setAttributes(screenLP);

        dlg.show();
    }

    public static void setStatus(XmppContact contact, View view) {
        GradientDrawable bg = ((GradientDrawable) view.getBackground().mutate());
        if(contact == null) {
            bg.setColor(ContactListAdapter.BACKGROUND_COLOR_DEAD);
        } else if (contact.isAvailable()) {
            if (contact.isAway())
                bg.setColor(ContactListAdapter.BACKGROUND_COLOR_STALE);
            else
                bg.setColor(ContactListAdapter.BACKGROUND_COLOR_ALIVE);
        } else {
            bg.setColor(ContactListAdapter.BACKGROUND_COLOR_DEAD);
        }
        bg.invalidateSelf();
    }

    private static void setTextView(TextView label, TextView view, String value, boolean bHideLabelIfEmpty) {
        if(bHideLabelIfEmpty && FileSystemUtils.isEmpty(value)){
            view.setVisibility(TextView.GONE);
            if(label != null)
                label.setVisibility(TextView.GONE);
            return;
        }

        view.setText(value);
    }

    @Override
    public boolean onVCardUpdate(final VCard card) {

        //see if we are showing this contact
        if(_dialog != null && _dialog.isShowing()
                && _displayed != null
                && card != null && card.getFrom() != null
                && FileSystemUtils.isEquals(_displayed.getId().toString(), card.getFrom().toString())) {

            //see if anything changed
            if(_card != null && card.equals(_card)){
                Log.d(TAG, "VCard has not changed for: " + card.getFrom());
                return true;
            }

            //refresh the dialog
            TAKChatUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TAKChatUtils.mapView.getContext(), "Refreshing profile...", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Refreshing dialog for: " + card.getFrom());
                    XmppContact temp = _displayed;
                    clear();
                    showContactInfo(temp, card, VCardManager.UpdateMode.None);
                }
            });
        }else{
            Log.d(TAG, "Skipping vcard update");
        }

        return true;
    }

    @Override
    public boolean onVCardSaved(VCard card) {
        return true;
    }

    @Override
    public boolean onVCardSaveFailed(VCard card) {
        return true;
    }

    @Override
    public void dispose() {

    }
}
