
package com.gmeci.atsk.obstructions.notification;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.bluetooth.BluetoothDevice;
import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.ATSKIntentConstants;
import com.gmeci.conversions.Conversions;
import java.util.List;
import com.atakmap.android.bluetooth.BluetoothManager;
import com.atakmap.android.bluetooth.BluetoothManager.BluetoothConnectionListener;
import com.gmeci.core.SurveyPoint;

public class NotificationController extends ImageView {

    private static final String TAG = "NotificationController";
    private static final int TOP_OFFSET = 6;
    private static final int HOLD_TIME_MS = 600;
    private static int TitleTextSize = 38;
    private static int StringTextSize = 28;
    private final Context _context;
    private final Context _mapContext;
    private SharedPreferences sharedPreferences;
    private Vibrator v;
    private boolean rtkBoxShowing = false;
    private Bitmap gps_status_bitmap, lrf_status_bitmap;
    private SurveyPoint _point;
    double Azimuth_deg, Elevation_deg, Range_m;
    boolean TopCollected;
    private String _gpsState = ATSKConstants.GPS_NO_CONNECTION;
    AlertDialog rtkLostDialog;
    boolean DisplayingLRF = false;
    String BubbleTitle = "";
    final String[] BubbleLines = new String[4];

    String[] serialList = null;

    boolean ButtonsOpened = false;
    FragmentActivity parentActivity;
    private final BroadcastReceiver gps_bt_StatusRx = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if (extras.containsKey(ATSKConstants.BT_CONNECTION_UPDATE_EXTRA)) {
                searchAndSetLRF(R.drawable.lrf_green);
            }
            if (extras.containsKey(ATSKConstants.GPS_ALERT)) {
                String GPSAlert = extras.getString(ATSKConstants.GPS_ALERT, "");
                if (GPSAlert.equals(ATSKConstants.GPS_ALERT_LOST_RTK)) {
                    //complain about lost GPS here!

                    v.vibrate(500);
                    synchronized (this) {
                        if (!rtkBoxShowing) {
                            rtkLostDialog.show();
                            rtkBoxShowing = true;
                            gps_status_bitmap = BitmapFactory.decodeResource(
                                    getResources(),
                                    R.drawable.gps_red);
                            NotificationController.this.invalidate();
                        }

                    }
                }
            }
            if (extras.containsKey(ATSKConstants.GMECI_GPS_QUALITY)) {
                String state = extras.getString(
                        ATSKConstants.GMECI_GPS_QUALITY,
                        ATSKConstants.GPS_NO_CONNECTION);
                if (!state.equals(_gpsState)) {
                    _gpsState = state;
                    //update what's on the screen
                    int resId = 0;
                    if (_gpsState.equals(ATSKConstants.GPS_EXTERNAL_RTK))
                        resId = R.drawable.gps_external_rtk;
                    else if (_gpsState.equals(ATSKConstants.GPS_EXTERNAL))
                        resId = R.drawable.gps_green;
                    else if (_gpsState.equals(ATSKConstants.GPS_INTERNAL))
                        resId = R.drawable.gps_yellow;
                    else if (_gpsState.equals(ATSKConstants.GPS_NO_CONNECTION))
                        resId = R.drawable.gps_red;

                    if (resId != 0)
                        gps_status_bitmap = BitmapFactory
                                .decodeResource(getResources(), resId);
                    NotificationController.this.invalidate();
                }
            }
        }

    };

    private final BroadcastReceiver serialManagerRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] arr = intent.getStringArrayExtra("active");
            serialList = arr;
            searchAndSetLRF(R.drawable.lrf_green);
        }
    };

    private final BroadcastReceiver notificationRx = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent i) {
            Bundle extras = i.getExtras();
            if (extras.containsKey(ATSKConstants.LRF_INPUT)) {
                DisplayingLRF = true;
                //show LRF shot and wait for approval to send response
                _point = i.getParcelableExtra(ATSKConstants.SURVEY_POINT_EXTRA);
                if (_point == null) {
                    _point = new SurveyPoint(
                            i.getDoubleExtra(ATSKConstants.LAT_EXTRA, 0),
                            i.getDoubleExtra(ATSKConstants.LON_EXTRA, 0));
                    _point.setHAE(i.getDoubleExtra(ATSKConstants.ALT_EXTRA, 0));
                }
                TopCollected = i.getBooleanExtra(ATSKIntentConstants.
                        OB_COLLECT_TOP, TopCollected);
                Range_m = i.getDoubleExtra(ATSKConstants.RANGE_M, Range_m);
                Azimuth_deg = i.getDoubleExtra(ATSKConstants.AZIMUTH_T,
                        Azimuth_deg);
                Elevation_deg = i.getDoubleExtra(ATSKConstants.ELEVATION,
                        Elevation_deg);

                String DisplayUnits = sharedPreferences
                        .getString(ATSKConstants.UNITS_DISPLAY,
                                ATSKConstants.UNITS_FEET);
                if (DisplayUnits.equals(ATSKConstants.UNITS_METERS)) {
                    BubbleLines[0] = String.format("   %.1fm @ %.1f%c Mag",
                            Range_m, Azimuth_deg, ATSKConstants.DEGREE_SYMBOL);
                } else {
                    BubbleLines[0] = String.format("   %.1f' @ %.1f%c Mag",
                            Range_m * Conversions.M2F, Azimuth_deg,
                            ATSKConstants.DEGREE_SYMBOL);
                }

                BubbleTitle = "    LRF SHOT";
                BubbleLines[3] = "";
                //        BubbleLines[0]=  String.format("   %.1fm @ %.1f%c Mag", Range_m, Azimuth_deg,DEGREE_SYMBOL);

                BubbleLines[1] = String.format(LocaleUtil.getCurrent(),
                        "   ALT: %.1fft", _point.getMSL() * Conversions.M2F);
                BubbleLines[2] = "   TAP TO APPROVE";

                setButtonOpened(true);

            } else if (extras.containsKey(ATSKConstants.NOTIFICATION_UPDATE)) {

                boolean AutoOpen = i.getBooleanExtra(
                        ATSKConstants.NOTIFICATION_AUTOOPEN, false);

                DisplayingLRF = false;
                BubbleTitle = i.getStringExtra(ATSKConstants.
                        NOTIFICATION_TITLE);
                BubbleLines[0] = i.getStringExtra(ATSKConstants.
                        NOTIFICATION_LINE1);
                BubbleLines[1] = i.getStringExtra(ATSKConstants.
                        NOTIFICATION_LINE2);
                BubbleLines[2] = i.getStringExtra(ATSKConstants.
                        NOTIFICATION_LINE3);
                BubbleLines[3] = "";
                if (AutoOpen)
                    setButtonOpened(true);
            }
            invalidate();
        }
    };
    private final Paint paintTitle = new Paint();
    private final Paint paintBody = new Paint();

    public NotificationController(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        _context = context;
        _mapContext = com.atakmap.android.maps.MapView.getMapView()
                .getContext();
        setupNotificationController();
    }

    public NotificationController(Context context, AttributeSet attrs) {
        super(context, attrs);
        _context = context;
        _mapContext = com.atakmap.android.maps.MapView.getMapView()
                .getContext();
        setupNotificationController();
    }

    public NotificationController(Context context) {
        super(context);
        _context = context;
        _mapContext = com.atakmap.android.maps.MapView.getMapView()
                .getContext();
        setupNotificationController();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        Log.d(TAG, "onLayout(" + changed + "," + left + "," + top + "," + right
                + "," + bottom);
    }

    public void dispose() {
        Pausing();
    }

    private void searchAndSetLRF(final int drawable) {
        post(new Runnable() {
            public void run() {
                Context pluginContext = ATSKApplication
                        .getInstance().getPluginContext();
                try {
                    List<BluetoothDevice> devices =
                            BluetoothManager.getInstance().getConnections();
                    for (BluetoothDevice device : devices) {
                        Log.d(TAG, "searching: " + device.getName());
                        if (device.getName().contains("TP360")) {
                            Log.d(TAG, "found lrf");
                            final Resources res = pluginContext.getResources();
                            lrf_status_bitmap = BitmapFactory.decodeResource(
                                    res,
                                    drawable);
                            invalidate();
                            return;
                        }
                    }
                    if (serialList != null) {
                        for (String serial : serialList) {
                            Log.d(TAG, "looking: " + serial);
                            if (serial.contains("Tru")) {
                                Log.d(TAG, "found serial lrf");
                                final Resources res = pluginContext
                                        .getResources();
                                lrf_status_bitmap = BitmapFactory
                                        .decodeResource(
                                                res,
                                                drawable);
                                invalidate();
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "error occured searching for LRF device", e);
                }
                final Resources res = pluginContext.getResources();
                Log.d(TAG,
                        "error occured searching for LRF device, turning red");
                lrf_status_bitmap = BitmapFactory.decodeResource(res,
                        R.drawable.lrf_red);
                invalidate();
            }
        });

    }

    private void setupNotificationController() {
        Context pluginContext = ATSKApplication
                .getInstance().getPluginContext();

        BluetoothManager.getInstance().addConnectionListener(bcl);
        AlertDialog.Builder editalert = new AlertDialog.Builder(_mapContext);
        editalert.setTitle("RTK Lost");
        editalert.setIcon(pluginContext.getResources().getDrawable(
                R.drawable.gps_red));
        editalert
                .setMessage("RTK Connection Lost, check distance to base, battery,etc");
        editalert.setPositiveButton("ACKNOWLEDGE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        synchronized (gps_bt_StatusRx) {
                            rtkBoxShowing = false;
                        }
                    }
                });
        rtkLostDialog = editalert.create();
        rtkLostDialog.setCancelable(false);

        v = (Vibrator) _context.getSystemService(Context.VIBRATOR_SERVICE);
        DocumentedIntentFilter responsefilter = new DocumentedIntentFilter();
        responsefilter.addAction(ATSKConstants.NOTIFICATION_BUBBLE);
        AtakBroadcast.getInstance().registerReceiver(notificationRx,
                responsefilter);

        DocumentedIntentFilter gps_bt_filter = new DocumentedIntentFilter();
        gps_bt_filter.addAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        gps_bt_filter.addAction(ATSKConstants.BT_DEVICE_CHANGE);
        gps_bt_filter.addAction(ATSKConstants.BT_LIST_REPOPULATED);

        AtakBroadcast.getInstance().registerSystemReceiver(gps_bt_StatusRx,
                gps_bt_filter);

        AtakBroadcast.getInstance().registerSystemReceiver(
                serialManagerRx,
                new DocumentedIntentFilter(
                        "com.partech.serialmanager.ActiveSerial"));

        Intent rasi = new Intent(
                "com.partech.serialmanager.RequestActiveSerial");
        AtakBroadcast.getInstance().sendSystemBroadcast(rasi);

        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(_mapContext);

        Resources res = pluginContext.getResources();
        //lrf_Bitmap = BitmapFactory.decodeResource(res, R.drawable.lrf_shot);
        gps_status_bitmap = BitmapFactory.decodeResource(res,
                R.drawable.gps_red);
        lrf_status_bitmap = BitmapFactory.decodeResource(res,
                R.drawable.lrf_red);

        searchAndSetLRF(R.drawable.lrf_green);

        paintTitle.setColor(0XFFFFFFFF);
        paintBody.setColor(0xFFFFFFFF);

        float TextSize = getResources().getDimensionPixelSize(
                R.dimen.notification_size);
        paintBody.setTextSize(TextSize * .75f);
        paintTitle.setTextSize(TextSize * 1.23f);
        TitleTextSize = (int) (TextSize * 1.23f);
        StringTextSize = (int) (TextSize * .85f);

        for (int i = 0; i < BubbleLines.length; i++) {
            BubbleLines[i] = "";
        }

        Intent btStatusRequestIntent = new Intent();
        btStatusRequestIntent.setAction(ATSKConstants.BT_STATUS_REQUEST);
        AtakBroadcast.getInstance().sendSystemBroadcast(btStatusRequestIntent);

    }

    public void clearText() {
        for (int i = 0; i < BubbleLines.length; i++) {
            BubbleLines[i] = "";
        }
        invalidate();
    }

    BluetoothConnectionListener bcl = new BluetoothConnectionListener() {
        public void connected(final BluetoothDevice bd) {
            Log.d(TAG, "connected: " + bd.getName());
            searchAndSetLRF(R.drawable.lrf_green);
        }

        public void disconnected(final BluetoothDevice bd) {
            Log.d(TAG, "disconnected: " + bd.getName());
            searchAndSetLRF(R.drawable.lrf_red);
        }

        public void error(final BluetoothDevice bd) {
            Log.d(TAG, "error: " + bd.getName());
            searchAndSetLRF(R.drawable.lrf_red);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (ButtonsOpened) {

            /*if (DisplayingLRF) {
                Log.d(TAG, "height:" + lrf_Bitmap.getHeight() + " width:"
                        + lrf_Bitmap.getWidth());
                Log.d(TAG,
                        "height scaled:" + lrf_Bitmap.getScaledHeight(canvas)
                                + " width:" + lrf_Bitmap.getScaledWidth(canvas));

                canvas.drawBitmap(lrf_Bitmap, 2 * getWidth() / 3f,
                        getHeight() / 2.2f, null);
            }*/

            //show gps and lrf bt status
            canvas.drawBitmap(gps_status_bitmap, 3 * getWidth() / 4f,
                    3f, null);

            canvas.drawBitmap(lrf_status_bitmap, 3 * getWidth() / 4f,
                    getHeight() - lrf_status_bitmap.getHeight() - 3, null);

            canvas.drawText(BubbleTitle, TitleTextSize, TitleTextSize
                    - TOP_OFFSET, paintTitle);

            for (int i = 0; i < BubbleLines.length; i++) {
                if (BubbleLines[i] != null) {
                    if (i == 1) {
                        canvas.drawText(BubbleLines[i], 1, TitleTextSize * 1.3f
                                + StringTextSize * (i) * 1.1f + 23, paintBody);
                    } else
                        canvas.drawText(BubbleLines[i], 1, TitleTextSize * 1.3f
                                + StringTextSize * (i) * 1.1f + 23, paintBody);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        double RealX = event.getX();
        int width = getWidth();
        boolean onUp = (event.getAction() & MotionEvent.ACTION_MASK)
                == MotionEvent.ACTION_UP;
        if (ButtonsOpened && onUp) {
            if (RealX > width * ((double) 7 / 8)) {
                setButtonOpened(false);
            } else if (RealX > width * ((double) 2 / 3)) {
                BTStatusDialog btsd = new BTStatusDialog();
                btsd.setupDialog(_context);
                FragmentManager manager = parentActivity
                        .getSupportFragmentManager();
                btsd.show(manager, "BT STATUS");
            } else if (DisplayingLRF) {
                setButtonOpened(false);
                //approve LRF
                Intent i = new Intent(ATSKConstants.
                        NOTIFICATION_BUBBLE_LRF_APPROVED);
                i.putExtra(ATSKConstants.SURVEY_POINT_EXTRA,
                        (Parcelable) _point);
                i.putExtra(ATSKConstants.LAT_EXTRA, _point.lat);
                i.putExtra(ATSKConstants.LON_EXTRA, _point.lon);
                i.putExtra(ATSKConstants.ALT_EXTRA, _point.getHAE());
                i.putExtra(ATSKConstants.CE_EXTRA, _point.circularError);
                i.putExtra(ATSKConstants.LE_EXTRA, _point.linearError);
                i.putExtra(ATSKConstants.RANGE_M, Range_m);
                i.putExtra(ATSKConstants.AZIMUTH_T, Azimuth_deg);
                i.putExtra(ATSKConstants.ELEVATION, Elevation_deg);
                i.putExtra(ATSKConstants.LRF_INPUT, true);
                i.putExtra(ATSKIntentConstants.OB_COLLECT_TOP, TopCollected);
                AtakBroadcast.getInstance().sendBroadcast(i);

                BubbleTitle = "LRF APPROVED";
                BubbleLines[2] = " APPROVED";
                invalidate();
            }
        } else {
            Log.d(TAG, "Touch Event. RealX:" + RealX + " getWidth:" + width);
            Log.d(TAG, "Action :" + event.getAction());

            if ((RealX < width / 4d) && onUp) {
                setButtonOpened(true);
                return true;
            }
        }
        if ((RealX > width / 4d) && !ButtonsOpened) {
            Log.d(TAG, "Well, you're close, not yet");
            return super.onTouchEvent(event);

        } else
            return true;//
        //return super.onTouchEvent(event);

    }

    public void setButtonOpened(boolean Opened) {
        ButtonsOpened = Opened;
        if (ButtonsOpened)
            setImageResource(R.drawable.smart_bubble);
        else
            setImageResource(R.drawable.smart_bubble_minimized);
    }

    public void Pausing() {
        if (_context == null)
            return;

        try {
            AtakBroadcast.getInstance().unregisterSystemReceiver(
                    gps_bt_StatusRx);
        } catch (Exception e) {
            Log.d(TAG, "error occurred unregistering the bt status receiver", e);
        }
        try {
            AtakBroadcast.getInstance().unregisterReceiver(notificationRx);
        } catch (Exception e) {
        }
    }

    public void Resuming() {
        if (_context == null)
            return;
        DocumentedIntentFilter resp = new DocumentedIntentFilter();
        resp.addAction(ATSKConstants.NOTIFICATION_BUBBLE);
        AtakBroadcast.getInstance()
                .registerSystemReceiver(notificationRx, resp);

        DocumentedIntentFilter gps_bt_filter = new DocumentedIntentFilter();
        gps_bt_filter.addAction(ATSKConstants.GMECI_HARDWARE_GPS_ACTION);
        gps_bt_filter.addAction(ATSKConstants.BT_DEVICE_CHANGE);
        gps_bt_filter.addAction(ATSKConstants.BT_LIST_REPOPULATED);

        AtakBroadcast.getInstance().registerSystemReceiver(gps_bt_StatusRx,
                gps_bt_filter);

    }

    public void setParent(FragmentActivity parentActivity) {
        this.parentActivity = parentActivity;
    }

}
