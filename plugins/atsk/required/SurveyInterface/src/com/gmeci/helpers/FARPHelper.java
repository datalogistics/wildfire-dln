
package com.gmeci.helpers;

import android.graphics.PointF;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;

import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.farp.FARPACParser;
import com.gmeci.atskservice.farp.FARPTankerItem;
import com.gmeci.conversions.Conversions;
import com.gmeci.csvparser.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;

public class FARPHelper {

    public static final String TAG = "FARPHelper";

    private final HashMap<String, ArrayList<PointF>> ACOutlineMap = new HashMap<String, ArrayList<PointF>>();
    private Map<String, FARPTankerItem> tankerList;

    public FARPHelper() {
    }

    public static LineObstruction BuildLineFromOutline(SurveyPoint startPoint,
            ArrayList<PointF> aCOutlineXYList) {
        if (aCOutlineXYList.size() == 0)
            return new LineObstruction();
        double CenterLat = startPoint.lat;
        double CenterLon = startPoint.lon;
        LineObstruction ACline = new LineObstruction();
        for (PointF nextPoint : aCOutlineXYList) {
            double[] Partial = Conversions.AROffset(CenterLat, CenterLon,
                    0 + startPoint.course_true + 180, nextPoint.y);
            double[] Partial2 = Conversions.AROffset(Partial[0], Partial[1],
                    90 + startPoint.course_true + 180, nextPoint.x);
            ACline.points.add(new SurveyPoint(Partial2[0], Partial2[1]));
        }
        ACline.points.add(ACline.points.get(0));
        return ACline;
    }

    public static ArrayList<PointF> loadACXY(String fileName,
            double FARPFuelOffset_m) {
        File zipFile;
        ArrayList<PointF> XYOutlineList = new ArrayList<PointF>();
        InputStreamReader isr = null;
        try {
            zipFile = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + fileName);

            isr = new InputStreamReader(new FileInputStream(
                    zipFile));

            CSVReader csvReader = new CSVReader(isr);
            //Skip the header
            csvReader.readNext();
            String[] currentRow;
            while ((currentRow = csvReader.readNext()) != null) {
                if (currentRow.length == 2) {
                    PointF nextPoint = new PointF(
                            Float.parseFloat(currentRow[0]),
                            (float) (Float.parseFloat(currentRow[1]) - FARPFuelOffset_m));
                    XYOutlineList.add(nextPoint);
                }
            }

            csvReader.close();
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception e) {
                    Log.d(TAG, "error closing the zip file", e);
                }
            }
        }
        return XYOutlineList;
    }

    public ArrayList<PointF> getACOutlineXY(String selectedAircraftName,
            boolean AllowOffset) {
        if (selectedAircraftName == null || selectedAircraftName.equals(""))
            selectedAircraftName = ATSKConstants.AC_C130;
        if (!ACOutlineMap.containsKey(selectedAircraftName)) {
            FARPTankerItem ti = getFARPTankerItem(selectedAircraftName);
            if (ti == null)
                return new ArrayList<PointF>();
            double Offset_y = ti.FuelPointOffset_m;
            if (!AllowOffset) {
                Offset_y = 0;
            }
            ACOutlineMap.put(selectedAircraftName,
                    loadACXY(ti.BlockFileName, Offset_y));
        }
        return ACOutlineMap.get(selectedAircraftName);
    }

    public FARPTankerItem getFARPTankerItem(String selectedAircraftName) {

        if (tankerList == null) {
            FARPACParser facp = new FARPACParser();
            try {
                tankerList = facp.parseFile();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //load AC Map
        if (tankerList != null) {
            for (FARPTankerItem ti : tankerList.values()) {
                if (selectedAircraftName == null
                        || ti.Name.equals(selectedAircraftName)) {
                    return ti;
                }
            }
            if (tankerList.size() > 0)
                // intended that this return the first one but the syntax was wrong
                // return tankerList.get(0);
                for (FARPTankerItem ti : tankerList.values()) {
                     return ti;
                }
        }
        return null;
    }
}
