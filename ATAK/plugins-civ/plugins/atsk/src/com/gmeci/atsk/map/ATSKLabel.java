
package com.gmeci.atsk.map;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyPoint;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Label marker with optional leader
 */
public class ATSKLabel extends ATSKMarker {

    public static final String TAG = "ATSKLabel";
    private final ObstructionProviderClient _opc;
    private final MapView _mapView;

    public ATSKLabel(String type, PointObstruction po) {
        super(type, po);
        _mapView = MapView.getMapView();
        _opc = new ObstructionProviderClient(_mapView.getContext());
        addOnGroupChangedListener(_onDeleted);
        setAlwaysShowText(true);
    }

    @Override
    public void setPoint(final GeoPoint point) {
        if (getPoint().distanceTo(point) >= 1.0)
            updateLeader(point);
        super.setPoint(point);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        List<LineObstruction> leaders = getLeaders();
        if (leaders != null) { 
            for (LineObstruction lo : leaders) {
                ATSKShape leader = ATSKShape.find(lo.uid);
                if (leader != null)
                    leader.setVisible(visible);
            }
        }
    }

    /**
     * Query database for all line leaders attached to this label
     * @return List of line leaders
     */
    public static List<LineObstruction> getLeaders(String uid,
            ObstructionProviderClient opc) {
        boolean stopOPC = !opc.isStarted();
        if (stopOPC)
            opc.Start();
        try {
            return opc.getLinesWithPrefix(ATSKConstants.DEFAULT_GROUP,
                    uid + ATSKConstants.LEADER_SUFFIX, true);
        } finally {
            if (stopOPC)
                opc.Stop();
        }
    }

    public List<LineObstruction> getLeaders() {
        return getLeaders(getUID(), _opc);
    }

    /**
     * Generate UID for a new line leader
     * @return New line leader UID
     */
    public static String getNewLeaderUID(String uid,
            ObstructionProviderClient opc) {
        int num = 0;
        List<LineObstruction> leaders = getLeaders(uid, opc);
        if (leaders != null) { 
            Collections.sort(leaders, new Comparator<LineObstruction>() {
                public int compare(LineObstruction a, LineObstruction b) {
                    int aNum = getLeaderNum(a);
                    int bNum = getLeaderNum(b);
                    return (aNum > bNum ? 1 : (aNum < bNum ? -1 : 0));
                }
            });
            for (LineObstruction lo : leaders) {
                if (getLeaderNum(lo) == num)
                    num++;
                else
                    break;
            }
        } 
        return uid + ATSKConstants.LEADER_SUFFIX + num;
    }

    public String getNewLeaderUID() {
        return getNewLeaderUID(getUID(), _opc);
    }

    private static int getLeaderNum(LineObstruction lo) {
        String num = lo.uid.substring(lo.uid
                .indexOf(ATSKConstants.LEADER_SUFFIX)
                + ATSKConstants.LEADER_SUFFIX.length());
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Update all line leader origin points
     * @param point Point to set as origin
     */
    public void updateLeader(GeoPoint point) {
        List<LineObstruction> leaders = getLeaders();
        if (leaders == null || leaders.isEmpty())
            return;
        if (_opc.Start()) {
            for (LineObstruction lo : leaders) {
                if (lo.points.size() > 0) {
                    // Last point should always be connected to label
                    SurveyPoint last = lo.points.get(lo.points.size() - 1);
                    last.setSurveyPoint(MapHelper.convertGeoPoint2SurveyPoint(
                            point));
                    // Update DB and polyline
                    _opc.EditLine(lo);
                }
            }
            _opc.Stop();
        }
    }

    // Delete leaders with label
    private final OnGroupChangedListener _onDeleted = new OnGroupChangedListener() {
        public void onItemAdded(MapItem item, MapGroup group) {
        }

        public void onItemRemoved(MapItem item, MapGroup group) {
            if (_opc.Start()) {
                if (_opc.GetPointObstruction(ATSKConstants.DEFAULT_GROUP,
                        getUID()) == null) {
                    Log.d(TAG, "Permanently removing leaders on " + getUID());
                    List<LineObstruction> leaders = getLeaders();
                    if (leaders != null) { 
                        for (LineObstruction lo : leaders)
                            _opc.DeleteLine(lo.group, lo.uid);
                    }
                }
                _opc.Stop();
            }
        }
    };
}
