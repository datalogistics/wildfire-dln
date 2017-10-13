
package com.gmeci.atsk.export;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import com.atakmap.android.ipc.AtakBroadcast;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyData.AZ_TYPE;
import com.gmeci.core.SurveyPoint;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.helpers.AZHelper;
import com.gmeci.conversions.Conversions;
import java.text.SimpleDateFormat;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.maps.coords.Altitude;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ODBGenerator {

    private static final String TAG = "ODBGenerator";
    final static double OBSTRUCTION_RANGE = 10000;
    Document document;
    SurveyData CurrentSurvey;
    ObstructionProviderClient opc;
    private final Context context;

    //private static final String COORDINATE_TIMESTAMP_FORMAT = "MM/dd/yyyy hh:mm:ss a";
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat(
            "MM/dd/yyyy hh:mm:ss a", LocaleUtil.getCurrent());

    public ODBGenerator(final Context context) {
        this.context = context;
    }

    protected void StartGeneration() {

        opc = new ObstructionProviderClient(context);
        opc.Start();
    }

    protected void StopGeneration() {
        opc.Stop();

    }

    protected void UpdateProgress(int Percentx100) {

    }

    public void GenerateAZODB(SurveyData currentSurvey, String FileName)
            throws ParserConfigurationException, TransformerException,
            IOException {

        StartGeneration();

        this.CurrentSurvey = currentSurvey;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
        document = documentBuilder.newDocument();
        Element rootElement = document.createElement("Document");

        AddElement(rootElement, "Name", CurrentSurvey.getSurveyName());
        AddElement(rootElement, "Owner", "ATSK");
        AddElement(rootElement, "DeviceType", "Trimble 5800");
        AddElement(rootElement, "EstimatedAccuracy",
                String.format("%.1f", CurrentSurvey.center.circularError));//LOU are the units right?

        double VariationValue = Conversions.GetTrueAngle(0,
                CurrentSurvey.center.lat, CurrentSurvey.center.lon);
        AddElement(rootElement, "MagneticVariation",
                String.format("%.1f", VariationValue));
        AddElement(rootElement, "CollectionMechanism", "Static");
        AddUnitsChild(rootElement);

        UpdateProgress(10);
        AddRunwaysChild(rootElement);
        UpdateProgress(20);
        if (CurrentSurvey.getType() == AZ_TYPE.LZ) {
            AddTaxiWaysChild(rootElement);
            UpdateProgress(40);
            AddApronsChild(rootElement);
            UpdateProgress(50);
            AddGradientsChild(rootElement);
        }
        UpdateProgress(60);
        AddObstaclesChild(rootElement);

        UpdateProgress(70);
        DOMSource domSource = new DOMSource(rootElement);
        OutputStream output = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(output);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Properties outFormat = new Properties();
        outFormat.setProperty(OutputKeys.INDENT, "yes");
        outFormat.setProperty(OutputKeys.METHOD, "xml");
        outFormat.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        outFormat.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        outFormat.setProperty(OutputKeys.VERSION, "1.0");
        outFormat.setProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(domSource, result);
        String xmlString = output.toString();

        UpdateProgress(80);
        writeToFile(xmlString, FileName);

        UpdateProgress(90);
        MediaScannerConnection.scanFile(context, new String[] {
                FileName
        }, null, null);

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(FileName)));
        AtakBroadcast.getInstance().sendBroadcast(intent);
        UpdateProgress(99);
        StopGeneration();
    }

    private void AddObstaclesChild(Element rootElement) {

        double outerRange = OBSTRUCTION_RANGE +
                (CurrentSurvey.circularAZ ? CurrentSurvey.getRadius() * 4
                        : CurrentSurvey.getLength() + CurrentSurvey.width);

        //draw a box 10km around the survey
        double North[] = Conversions.AROffset(CurrentSurvey.center.lat,
                CurrentSurvey.center.lon, 0, outerRange);
        double South[] = Conversions.AROffset(CurrentSurvey.center.lat,
                CurrentSurvey.center.lon, 180, outerRange);
        double East[] = Conversions.AROffset(CurrentSurvey.center.lat,
                CurrentSurvey.center.lon, 90, outerRange);
        double West[] = Conversions.AROffset(CurrentSurvey.center.lat,
                CurrentSurvey.center.lon, 270, outerRange);

        Cursor pointCursor = null;
        Element Obstacles = document.createElement("Obstacles");

        try {
            pointCursor = opc.GetAllPointsBounded(North[0], South[0],
                    West[1], East[1], null);
            if (pointCursor != null && pointCursor.getCount() > 0) {
                for (pointCursor.moveToFirst(); !pointCursor.isAfterLast(); pointCursor
                        .moveToNext()) {
                    PointObstruction target = opc
                            .GetPointObstruction(pointCursor);
                    if (target != null)
                        AddPointObstacle(Obstacles, target, null);
                }

            }
        } finally {
            if (pointCursor != null)
                pointCursor.close();
        }

        Cursor lineCursor = opc.GetAllLinesBounded(ATSKConstants.DEFAULT_GROUP,
                North[0], South[0], West[1], East[1]);
        if (lineCursor != null && lineCursor.getCount() > 0) {
            for (lineCursor.moveToFirst(); !lineCursor.isAfterLast(); lineCursor
                    .moveToNext()) {
                LineObstruction nextLine = opc.GetLineObstruction(lineCursor);
                if (nextLine != null)
                    AddLineObstacle(Obstacles, nextLine);
            }
        }
        if (lineCursor != null)
            lineCursor.close();

        if (CurrentSurvey.getType() == AZ_TYPE.HLZ) {
            PointObstruction HLZCenterPO = new PointObstruction(
                    CurrentSurvey.center);
            HLZCenterPO.type = "HLZ-Center Point";
            HLZCenterPO.uid = "HLZ-Center Point";
            AddPointObstacle(Obstacles, HLZCenterPO, null);
        } else if (CurrentSurvey.getType() == AZ_TYPE.DZ) {

            PointObstruction PO = new PointObstruction(
                    CurrentSurvey.pointOfOrigin);
            PO.type = "DZ: Point of Origin (Dir/Dist to PI)";
            PO.uid = "DZ: Point of Origin (Dir/Dist to PI)";

            PropertyListItem distPLI = new PropertyListItem();
            PropertyListItem headingPLI = new PropertyListItem();
            double RAB[] = Conversions.CalculateRangeAngle(
                    CurrentSurvey.pointOfOrigin.lat,
                    CurrentSurvey.pointOfOrigin.lon, CurrentSurvey.center.lat,
                    CurrentSurvey.center.lon);

            if (RAB[0] < 10000) {
                distPLI.Value = String.format("%.1f", RAB[0] * Conversions.M2F
                        / 3f);
                distPLI.Name = "Dist to PI (Yds)";
                headingPLI.Value = String.format("%.1f", Conversions
                        .GetMagAngle((float) RAB[1], CurrentSurvey.center.lat,
                                CurrentSurvey.center.lon));
                headingPLI.Name = "HDG (Mag) to PI";
                ArrayList<PropertyListItem> plList = new ArrayList<PropertyListItem>();
                plList.add(distPLI);
                plList.add(headingPLI);
                AddPointObstacle(Obstacles, PO, plList);
            }
        }

        rootElement.appendChild(Obstacles);

    }

    private void AddLineObstacle(Element obstacles,
            LineObstruction lineObstruction) {

        Element Obstacle = document.createElement("Obstacle");
        Element Coordinates = document.createElement("Coordinates");

        AddElement(Obstacle, "Type", lineObstruction.type);
        if (lineObstruction.closed)
            AddElement(Obstacle, "GeometryType", "Area");
        else
            AddElement(Obstacle, "GeometryType", "Route");

        //get points out of db
        for (int i = 0; i < lineObstruction.points.size(); i++)
            AddCoordinate2Coordinates(Coordinates,
                    lineObstruction.points.get(i).lat,
                    lineObstruction.points.get(i).lon,
                    lineObstruction.points.get(i).getHAE());
        Obstacle.appendChild(Coordinates);

        AddProperty(Obstacle, "Units", "Feet");
        AddProperty(Obstacle, "Capture", "Base");

        AddProperty(Obstacle, "Width", String.format("%.1f",
                lineObstruction.width * Conversions.M2F));
        AddProperty(
                Obstacle,
                "Height",
                String.format("%.1f", lineObstruction.height
                        * Conversions.M2F));

        obstacles.appendChild(Obstacle);

    }

    private void AddCoordinate2Coordinates(Element Coordinates, double lat,
            double lon, double hae) {

        Element Coordinate = document.createElement("Coordinate");
        String Northing = Conversions.GetUTMNorthing(lat, lon);
        String Easting = Conversions.GetUTMEasting(lat, lon);

        if (Double.compare(Altitude.UNKNOWN.getValue(), hae) == 0)
            hae = 0;

        AddElement(Coordinate, "Y", Northing);
        AddElement(Coordinate, "X", Easting);
        AddElement(Coordinate, "Z",
                String.format("%.2f", hae * Conversions.M2F));

        AddElement(Coordinate, "Latitude", String.format("%.9f", lat));
        AddElement(Coordinate, "Longitude", String.format("%.9f", lon));
        AddElement(Coordinate, "Elevation",
                String.format("%.2f", hae * Conversions.M2F));

        synchronized (timeFormatter) { 
            String NowTimeString = timeFormatter.format(CoordinatedTime
                .currentDate());
            AddElement(Coordinate, "GPSTime", NowTimeString);
        }
        Coordinates.appendChild(Coordinate);

    }

    private void AddPointObstacle(Element obstacles,
            PointObstruction pointObstruction,
            ArrayList<PropertyListItem> PropertyList) {

        Element Obstacle = document.createElement("Obstacle");
        Element Coordinates = document.createElement("Coordinates");

        AddElement(Obstacle, "Type", pointObstruction.type);
        AddElement(Obstacle, "GeometryType", "Point");

        AddCoordinate2Coordinates(Coordinates, pointObstruction.lat,
                pointObstruction.lon, pointObstruction.getHAE());
        Obstacle.appendChild(Coordinates);

        AddProperty(Obstacle, "Capture", "Base");
        //    AddProperty(Obstacle, "Length", String.format("%.1f",pointObstruction.length* Conversions.M2F));
        double Length = (float) (pointObstruction.length * Conversions.M2F);
        if (Length < 1)
            Length = 1;
        AddProperty(Obstacle, "Length", String.format("%.1f", Length));//converted to METERS out of tomfoolery

        double Width = (float) (pointObstruction.width * Conversions.M2F);
        if (Width < 1)
            Width = 1;
        AddProperty(Obstacle, "Width", String.format("%.1f", Width));
        AddProperty(
                Obstacle,
                "Height",
                String.format("%.1f", pointObstruction.height
                        * Conversions.M2F));
        AddProperty(Obstacle, "Units", "Feet");
        AddProperty(Obstacle, "Rotation",
                String.format("%.1f", pointObstruction.course_true));

        if (PropertyList != null) {
            for (PropertyListItem pli : PropertyList) {
                Element Property = document.createElement("Property");
                AddElement(Property, "Name", pli.Name);
                AddElement(Property, "Value", pli.Value);
                Obstacle.appendChild(Property);
            }
        }

        obstacles.appendChild(Obstacle);

    }

    private void AddProperty(Element obstacle, String PropertyName,
            String PropertyValue) {

        Element Property = document.createElement("Property");
        AddElement(Property, "Name", PropertyName);
        AddElement(Property, "Value", PropertyValue);
        obstacle.appendChild(Property);
    }

    private void AddApronsChild(Element rootElement) {

        Element Aprons = document.createElement("Aprons");
        Element Apron = document.createElement("Apron");

        Aprons.appendChild(Apron);
        rootElement.appendChild(Aprons);

    }

    private void AddGradientsChild(Element rootElement) {
        //get analyzed graidents to put here - no raw gradients
        Element Gradients = document.createElement("Gradients");
        GradientProviderClient gpc = new GradientProviderClient(context);
        gpc.Start();

        Cursor gradientCursor = null;
        try {
            gradientCursor = gpc.GetAnalyzedGradients(CurrentSurvey.uid,
                    true);
            if (gradientCursor != null && gradientCursor.getCount() > 0) {
                for (gradientCursor.moveToFirst(); !gradientCursor
                        .isAfterLast(); gradientCursor
                        .moveToNext()) {
                    GradientDBItem currentLine = gpc.GetGradient(
                            gradientCursor,
                            true);
                    //draw on the map...
                    List<SurveyPoint> Points = currentLine.getPoints();
                    if (Points != null) {
                        Element Gradient = document.createElement("Gradient");
                        for (SurveyPoint NextPoint : Points) {
                            AddCoordinate(Gradient, NextPoint.lat,
                                    NextPoint.lon,
                                    NextPoint.getHAE());
                        }
                        Gradients.appendChild(Gradient);
                    }
                }
            }
        } finally {
            gpc.Stop();
            if (gradientCursor != null)
                gradientCursor.close();
        }
        rootElement.appendChild(Gradients);
    }

    private void AddTaxiWaysChild(Element rootElement) {
        Element Taxiways = document.createElement("Taxiways");
        Element Taxiway = document.createElement("Taxiway");

        Taxiways.appendChild(Taxiway);
        rootElement.appendChild(Taxiways);
    }

    private void AddRunwaysChild(Element rootElement) {

        Element CriteriaBlockElement = document.createElement("CriteriaBlock");
        if (CurrentSurvey.getType() == AZ_TYPE.LZ) {

            Element Runways = document.createElement("Runways");
            Element Runway = document.createElement("Runway");
            int NameAngle = (int) (CurrentSurvey.angle / 10);
            int StartAngle, EndAngle;
            if (NameAngle > 18) {
                EndAngle = NameAngle;
                StartAngle = NameAngle - 18;
            } else {
                StartAngle = NameAngle;
                EndAngle = NameAngle - 18;
                if (EndAngle < 0)
                    EndAngle += 36;
            }
            AddElement(Runway, "Name",
                    String.format("%d-%d", StartAngle, EndAngle));
            AddElement(Runway, "Condition", "");
            AddElement(
                    Runway,
                    "Width",
                    String.format("%.1f", CurrentSurvey.width
                            * Conversions.M2F));

            //double ApproachCenter[] = Conversions.AROffset(survey.center.lat, survey.center.lon, survey.angle, (float)(survey.getLength(false)/2.0));
            SurveyPoint approachCenter = AZHelper.CalculateCenterOfEdge(
                    CurrentSurvey, true);
            Element ApproachCenterElement = document
                    .createElement("ApproachCenter");
            AddCoordinate(ApproachCenterElement, approachCenter.lat,
                    approachCenter.lon, CurrentSurvey.center.getHAE());
            Runway.appendChild(ApproachCenterElement);

            //double DepartureCenter[] = Conversions.AROffset(survey.center.lat, survey.center.lon, 180+ survey.angle, (float)(survey.getLength(false)/2.0));
            SurveyPoint departureCenter = AZHelper.CalculateCenterOfEdge(
                    CurrentSurvey, false);
            Element DepartureCenterElement = document
                    .createElement("DepartureCenter");
            AddCoordinate(DepartureCenterElement, departureCenter.lat,
                    departureCenter.lon, CurrentSurvey.center.getHAE());
            Runway.appendChild(DepartureCenterElement);

            AddElement(Runway, "SurfaceType", CurrentSurvey.surface);
            AddElement(CriteriaBlockElement, "AircraftName",
                    CurrentSurvey.aircraft);
            Runway.appendChild(CriteriaBlockElement);
            Runways.appendChild(Runway);
            rootElement.appendChild(Runways);
        } else if (CurrentSurvey.getType() == AZ_TYPE.HLZ) {
            AddElement(CriteriaBlockElement, "HLZType",
                    CurrentSurvey.circularAZ ? "Circular" : "Rectangular");

            Element HLZCenterElement = document
                    .createElement("HelicopterLandingZoneCenter");
            AddCoordinate(HLZCenterElement, CurrentSurvey.center.lat,
                    CurrentSurvey.center.lon, CurrentSurvey.center.getHAE());
            CriteriaBlockElement.appendChild(HLZCenterElement);
            if (CurrentSurvey.circularAZ) {
                AddElement(
                        CriteriaBlockElement,
                        "Radius",
                        String.format("%.1f", CurrentSurvey.getRadius()
                                * Conversions.M2F));
            } else {
                AddElement(
                        CriteriaBlockElement,
                        "Length",
                        String.format("%.1f", CurrentSurvey.getLength(false)
                                * Conversions.M2F));
                AddElement(
                        CriteriaBlockElement,
                        "Width",
                        String.format("%.1f", CurrentSurvey.width
                                * Conversions.M2F));
                AddElement(CriteriaBlockElement, "Heading",
                        String.format("%.1f", CurrentSurvey.angle));
            }
            AddElement(CriteriaBlockElement, "ApproachHeading",
                    String.format("%.1f", CurrentSurvey.approachAngle));
            AddElement(CriteriaBlockElement, "DepartureHeading",
                    String.format("%.1f", CurrentSurvey.departureAngle));

            Element CriteriaBlocksElement = document
                    .createElement("CriteriaBlocks");
            CriteriaBlocksElement.appendChild(CriteriaBlockElement);
            rootElement.appendChild(CriteriaBlocksElement);
        } else if (CurrentSurvey.getType() == AZ_TYPE.DZ) {

            AddElement(CriteriaBlockElement, "DZType",
                    CurrentSurvey.circularAZ ? "Circular" : "Rectangular");
            if (CurrentSurvey.circularAZ) {
                AddElement(
                        CriteriaBlockElement,
                        "Radius",
                        String.format("%.1f", CurrentSurvey.getRadius()
                                * Conversions.M2F));
            }
            {//rectangular
                SurveyPoint center_LE = AZHelper.CalculateCenterOfEdge(
                        CurrentSurvey, true);
                Element DZLECenterElement = document
                        .createElement("DropZoneLeadingEdgeCenter");
                AddCoordinate(DZLECenterElement, center_LE.lat, center_LE.lon,
                        center_LE.getHAE());
                CriteriaBlockElement.appendChild(DZLECenterElement);

                AddElement(
                        CriteriaBlockElement,
                        "Length",
                        String.format("%.1f", CurrentSurvey.getLength(false)
                                * Conversions.M2F));
                AddElement(
                        CriteriaBlockElement,
                        "Width",
                        String.format("%.1f", CurrentSurvey.width
                                * Conversions.M2F));
                AddElement(
                        CriteriaBlockElement,
                        "Heading",
                        String.format("%.1f", CurrentSurvey.angle
                                * Conversions.M2F));
            }
            Element DZCenterElement = document.createElement("DropZoneCenter");
            AddCoordinate(DZCenterElement, CurrentSurvey.center.lat,
                    CurrentSurvey.center.lon, CurrentSurvey.center.getHAE());
            CriteriaBlockElement.appendChild(DZCenterElement);

            Element PERPI = document.createElement("PointOfImapactPersonnel");
            SurveyPoint perpi = AZHelper.CalculatePointOfImpact(CurrentSurvey,
                    "per");
            AddCoordinate(PERPI, perpi.lat, perpi.lon, perpi.getHAE());
            CriteriaBlockElement.appendChild(PERPI);

            Element CDSPI = document
                    .createElement("PointOfImapactContainerDeliverySystem");
            SurveyPoint cdspi = AZHelper.CalculatePointOfImpact(CurrentSurvey,
                    "cds");
            AddCoordinate(CDSPI, cdspi.lat, cdspi.lon, cdspi.getHAE());
            CriteriaBlockElement.appendChild(CDSPI);

            Element HEPI = document
                    .createElement("PointOfImapactHeavyEquipment");
            SurveyPoint hepi = AZHelper.CalculatePointOfImpact(CurrentSurvey,
                    "he");
            AddCoordinate(HEPI, hepi.lat, hepi.lon, hepi.getHAE());
            CriteriaBlockElement.appendChild(HEPI);

            Element CriteriaBlocksElement = document
                    .createElement("CriteriaBlocks");
            CriteriaBlocksElement.appendChild(CriteriaBlockElement);
            rootElement.appendChild(CriteriaBlocksElement);

        }

    }

    private void AddCoordinate(Element coordinateParent, double lat,
            double lon, double alt_m) {
        Element Coordinate = document.createElement("Coordinate");
        AddElement(Coordinate, "Latitude", String.format("%.9f", lat));
        AddElement(Coordinate, "Longitude", String.format("%.9f", lon));

        // adjust for unknown altitude
        if (Double.compare(Altitude.UNKNOWN.getValue(), alt_m) == 0)
            alt_m = 0;

        AddElement(Coordinate, "Elevation", String.format("%.1f", alt_m));
        String UTMEasting = Conversions.GetUTMEasting(lat, lon);
        String UTMNorthing = Conversions.GetUTMNorthing(lat, lon);

        AddElement(Coordinate, "X", UTMEasting);//LOU Is this X or Y
        AddElement(Coordinate, "Y", UTMNorthing);//LOU Is this X or Y
        AddElement(Coordinate, "Z",
                String.format("%.1f", alt_m * Conversions.M2F));

        synchronized (timeFormatter) { 
            String NowTimeString = timeFormatter.format(CoordinatedTime
                .currentDate());
            AddElement(Coordinate, "GPSTime", NowTimeString);
        }
        coordinateParent.appendChild(Coordinate);

    }

    private void AddUnitsChild(Element rootElement) {
        Element Units = document.createElement("Units");

        String RealUTMZone = Conversions.GetUTMHemisphereZone(
                CurrentSurvey.center.lat, CurrentSurvey.center.lon);

        //UTM Zone comes out looking like this [N/S]##   and needs to look like ##[N/S]
        //String RealUTMZone = String.format("%c%c%c", UTMZone.charAt(0), UTMZone.charAt(1), UTMZone.charAt(3) );

        RealUTMZone = RealUTMZone.replace("N", "North");
        RealUTMZone = RealUTMZone.replace("S", "South");
        AddElement(Units, "UTMZone", RealUTMZone);
        AddElement(Units, "CoordinateUnits", "Meters");
        AddElement(Units, "ElevationUnits", "Feet");

        rootElement.appendChild(Units);

    }

    private void AddElement(Element rootElement, String ElementName,
            String ElementValue) {
        Element Owner = document.createElement(ElementName);
        Owner.setTextContent(ElementValue);
        rootElement.appendChild(Owner);
    }

    private void writeToFile(String data, String FileName) throws IOException {

        File xmlfile = new File(FileName);
        if (!xmlfile.getParentFile().mkdirs()) {
            Log.d(TAG, " Failed to make dir at " +
                    xmlfile.getParentFile().getAbsolutePath());
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(xmlfile);
            writer.append(data);
            writer.flush();
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    static class PropertyListItem {
        String Name, Value;
    }
}
