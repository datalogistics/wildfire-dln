
package com.gmeci.atsk.export;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.hierarchy.HierarchyListAdapter;
import com.atakmap.android.hierarchy.filters.FOVFilter;
import com.atakmap.android.imagecapture.ImageCapture;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.Arrow;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.toolbar.ToolbarBroadcastReceiver;
import com.atakmap.android.toolbars.RangeAndBearingEndpoint;
import com.atakmap.android.util.ATAKConstants;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.DistanceCalculations;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.ekito.simpleKML.model.LatLonAltBox;
import com.ekito.simpleKML.model.LookAt;
import com.ekito.simpleKML.model.Region;
import com.gmeci.atsk.MapHelper;
import com.gmeci.atsk.export.imagecapture.SurveyCaptureToolbar;
import com.gmeci.atsk.gallery.ATSKGalleryExport;
import com.gmeci.atsk.gallery.ATSKGalleryItem;
import com.gmeci.atsk.gallery.ATSKGalleryUtils;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.gmeci.atsk.gallery.ATSKMarkup;
import com.gmeci.atsk.map.ATSKLineLeader;
import com.gmeci.atsk.map.ATSKMarker;
import com.gmeci.atsk.map.ATSKShape;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.ShapeFile;
import com.gmeci.core.ATSKConstants;

import com.atakmap.android.importexport.FormatNotSupportedException;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.spatial.kml.KMLUtil;
import com.ekito.simpleKML.Serializer;
import com.ekito.simpleKML.model.Document;
import com.ekito.simpleKML.model.Feature;
import com.ekito.simpleKML.model.Folder;
import com.ekito.simpleKML.model.Kml;
import com.ekito.simpleKML.model.StyleSelector;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.PointObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.atsk.ATSKATAKConstants;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.ObstructionProviderClient;
import com.gmeci.atskservice.resolvers.GradientDBItem;
import com.gmeci.atskservice.resolvers.GradientProviderClient;

import java.io.FilenameFilter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.atakmap.android.missionpackage.api.MissionPackageApi;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.importfiles.sort.ImportInPlaceResolver;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;

import android.text.format.DateFormat;
import android.widget.Toast;

/**
 * This will wrap the ATSKMissionPackageReceiver and ATSKMissionPackageSender
 */
public class ATSKMissionPackageManager {

    private static final String TAG = "ATSKMissionPackageManager";

    final static String DumpName = "transfer.atsk";
    private final MapView _mapView;
    private final Context _plugin;
    private final ObstructionProviderClient _opc;
    private final AZProviderClient _azpc;
    private final GradientProviderClient _gpc;
    private SurveyCaptureToolbar _captureToolbar;

    private static ATSKMissionPackageManager _instance;

    public static class ATSKImport extends ImportInPlaceResolver {
        final static String TAG = "ATSKImport";
        private final MapView _mapView;
        private final ATSKMissionPackageManager _ampm;

        public ATSKImport(final MapView mp, final ATSKMissionPackageManager ampm) {
            super(".atsk", "atsk", true, false, true, "Surveys");
            _mapView = mp;
            _ampm = ampm;
        }

        @Override
        public boolean match(final File file) {
            boolean retval = super.match(file);
            Log.d(TAG, "match: " + file + " " + retval);
            return retval;
        }

        @Override
        protected void onFileSorted(final File src, File dst,
                Set<SortFlags> flags) {
            super.onFileSorted(src, dst, flags);

            Log.d(TAG, "begin import: " + src);

            _ampm.surveyReceived(src.toString());
            _mapView.post(new Runnable() {
                public void run() {
                    Toast.makeText(_mapView.getContext(),
                            "Imported ATSK survey: " + src.getName(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public ATSKMissionPackageManager(final MapView view, final Context plugin) {
        _mapView = view;
        _plugin = plugin;

        _opc = new ObstructionProviderClient(_mapView.getContext());
        _opc.Start();
        _azpc = new AZProviderClient(_mapView.getContext());
        _azpc.Start();
        _gpc = new GradientProviderClient(_mapView.getContext());
        _gpc.Start();

        ImportExportMapComponent.getInstance().addImporterClass(
                new ATSKImport(view, this));

        _instance = this;
    }

    public static ATSKMissionPackageManager getInstance() {
        return _instance;
    }

    public void Stop() {
        _opc.Stop();
        _azpc.Stop();
        _gpc.Stop();

        dispose();
    }

    public void dispose() {
    }

    // Send survey + gradients + obstructions
    public void send(final String uid) {
        ATSKTransferPackage pkg = new ATSKTransferPackage();
        SurveyData survey = _azpc.getAZ(uid, false);
        pkg.surveyList.add(survey);
        pkg.addSurveyObstructions(survey);
        pkg.addSurveyGradients(survey, _gpc);
        Log.d(TAG, "built mission package for: " + uid);
        makeAndSendPackage(pkg);
        Log.d(TAG, "sent package for: " + uid);
    }

    // Send gradients, obstructions, or both without survey
    public void send(String surveyUID, boolean expSurvey, boolean expGrad,
                     boolean expObs) {
        if (!expSurvey && !expGrad && !expObs) {
            toast("No survey items selected");
            return;
        }
        ATSKTransferPackage pkg = new ATSKTransferPackage();
        SurveyData survey = _azpc.getAZ(surveyUID, false);
        if (expSurvey)
            pkg.surveyList.add(survey);
        if (expGrad)
            pkg.addSurveyGradients(survey, _gpc);
        if (expObs)
            pkg.addSurveyObstructions(survey);
        makeAndSendPackage(pkg);
    }

    public void save(final String uid) {
        ATSKTransferPackage pkg = new ATSKTransferPackage();
        SurveyData survey = _azpc.getAZ(uid, false);
        pkg.surveyList.add(survey);
        pkg.addSurveyObstructions(survey);
        pkg.addSurveyGradients(survey, _gpc);
        pkg.addGallery(uid, ATSKGalleryUtils.getImages(uid));
        saveGallery(uid, null, false, true, true);

        Log.d(TAG, "export file: " + uid);
        String file = getFileName(uid, ".atsk");
        makeAndSave(pkg, file);
        Log.d(TAG, "saved package for: " + uid);
        toast("Exported survey to " + file);
    }

    public void saveGallery(final String uid, final File[] imageList,
            final boolean notify, boolean markup, boolean pdf) {
        File[] imgs = (imageList != null ? imageList : ATSKGalleryUtils
                .getImages(uid));
        if (pdf) {
            // Find latest survey capture
            SurveyData survey = _azpc.getAZ(uid, false);
            if (survey != null) {
                File expDir = new File(getFileName(uid, "")).getParentFile();
                final String prefix = String.format("%s_%s_",
                        survey.getSurveyName(),
                        survey.getType().toString());
                File[] expFiles = expDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.startsWith(prefix)
                                && (filename.endsWith(".jpg")
                                || filename.endsWith(".png"));
                    }
                });
                if (expFiles != null && expFiles.length > 0) {
                    // Sort modification date descending
                    Arrays.sort(expFiles, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            long lmod = lhs.lastModified(),
                            rmod = rhs.lastModified();
                            if (lmod > rmod)
                                return -1;
                            else if (lmod < rmod)
                                return 1;
                            return 0;
                        }
                    });
                    File latest = expFiles[0];
                    // Set markup to true in case it's not already
                    if (!ATSKMarkup.upToDate(latest))
                        ATSKMarkup.setMarkup(latest, latest, true);
                    // Insert newest capture into PDF
                    File[] expanded = new File[imgs.length + 1];
                    System.arraycopy(imgs, 0, expanded, 0, imgs.length);
                    expanded[imgs.length] = latest;
                    imgs = expanded;
                }
            }
        }

        if (imgs.length > 0) {
            final File pdfDir = new File(getFileName(uid, "-gallery.pdf"));
            final File dir = pdfDir.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs())
                    Log.d(TAG, "Failed to make dir at " + dir.getAbsolutePath());
            }
            ATSKGalleryExport eit = new ATSKGalleryExport(
                    _mapView, imgs, dir, markup, pdf ? pdfDir : null);
            eit.start(new ATSKGalleryExport.Callback() {
                @Override
                public void onFinish(List<File> exported) {
                    if (notify)
                        toast("Saved survey image gallery to "
                                + dir.getAbsolutePath());
                }
            });
        } else {
            if (notify)
                toast("Survey image gallery is empty.");
        }
    }

    public static String getFileName(SurveyData survey, String filetype) {
        if (survey == null)
            return ATSKConstants.ATSK_SURVEY_FOLDER_BASE;
        //make sure we have a folder with the right name...

        final String fname = String.format(
                "%s_%s_%s" + filetype,
                survey.getSurveyName(),
                survey.getType().toString(),
                DateFormat.format("yyyyMMdd'T'HHmmss",
                        CoordinatedTime.currentDate()));

        File f = new File(ATSKConstants.ATSK_SURVEY_FOLDER_BASE
                + File.separator +
                survey.getType().toString() + File.separator +
                survey.getSurveyName());
        f = new File(f, fname);
        return f.getAbsolutePath();
    }

    private String getFileName(String uid, String filetype) {
        return getFileName(_azpc.getAZ(uid, false), filetype);
    }

    public void save(String surveyUID, boolean expSurvey, boolean expGrad,
                     boolean expObs) {
        if (!expSurvey && !expGrad && !expObs) {
            toast("No survey items selected");
            return;
        }

        ATSKTransferPackage pkg = new ATSKTransferPackage();
        SurveyData survey = _azpc.getAZ(surveyUID, false);
        if (expSurvey)
            pkg.surveyList.add(survey);
        if (expGrad)
            pkg.addSurveyGradients(survey, _gpc);
        if (expObs)
            pkg.addSurveyObstructions(survey);

        String prefix = "";
        if (!expSurvey) {
            if (expGrad)
                prefix += "_gradients";
            if (expObs)
                prefix += "_obstruction";
        }

        String file = getFileName(surveyUID, prefix + ".atsk");
        Log.d(TAG, "saved package: " + file);
        makeAndSave(pkg, file);
        toast("Exported survey to " + file);
    }

    private void makeAndSendPackage(ATSKTransferPackage fullDB) {

        String DBPackageString = Conversions.toJson(fullDB);
        String dbFileName = null;
        String folderName = Environment.getExternalStorageDirectory()
                + File.separator + "atsk" + File.separator + "atskMP";
        String fullFileName = folderName + File.separator + DumpName;
        try {
            dbFileName = writeFile(fullFileName, DBPackageString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dbFileName != null) {
            MissionPackageManifest mpm = MissionPackageApi
                    .CreateTempManifest("atsk");
            mpm.addFile(new File(dbFileName), null);
            MissionPackageApi.Send(_mapView.getContext(), mpm, null, null);
        }
    }

    private void makeAndSave(ATSKTransferPackage fullDB, String filename) {
        String DBPackageString = Conversions.toJson(fullDB);
        try {
            writeFile(filename, DBPackageString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String writeFile(String filename, String surveyPackageString)
            throws IOException {

        File file = createFile(filename);

        //write the bytes in file
        if (file.exists()) {
            OutputStream fo = null;
            try {
                fo = new FileOutputStream(file);
                fo.write(surveyPackageString.getBytes(
                        FileSystemUtils.UTF8_CHARSET));
            } finally {
                if (fo != null) {
                    try {
                        fo.close();
                    } catch (IOException e) {
                    }
                }
            }
            Log.d(TAG, "file created: " + file);
        }

        return filename;
    }

    private File createFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            Log.w(TAG, "Failed to create directories: "
                    + file.getParentFile().getAbsolutePath());

        if (!file.createNewFile())
            Log.w(TAG, "Failed to create file: " + file.getAbsolutePath());

        return file;
    }

    public void surveyReceived(String incomingDBFileName) {
        readNewDBFile(incomingDBFileName);
    }

    private void readNewDBFile(String incomingDBFileName) {
        ATSKTransferPackage fullDB;
        fullDB = ReadFromFile(incomingDBFileName);

        if (fullDB == null) {
            toast("Failed to import survey: " + incomingDBFileName);
            return;
        }

        for (GradientDBItem currentDBItem : fullDB.gradients)
            _gpc.NewGradient(currentDBItem.toLO(true));

        for (SurveyData currentSurvey : fullDB.surveyList)
            _azpc.NewAZ(currentSurvey);

        for (PointObstruction currentPO : fullDB.points)
            _opc.NewPoint(currentPO);

        for (LineObstruction currentLO : fullDB.lines)
            _opc.NewLine(currentLO);

        String dir = new File(incomingDBFileName).getParent();
        for (ATSKGalleryItem item : fullDB.gallery) {
            String err = ATSKGalleryUtils.importImage(item.surveyUID,
                    dir + File.separator + item.path);
            if (err != null)
                Log.e(TAG, "Failed to import image specified in DB: " + err);
        }
    }

    protected ATSKTransferPackage ReadFromFile(String FileName) {
        File mpDbjFile = new File(FileName);
        //if file exists, use these criteria.  if not, use Criteria.txt
        if (mpDbjFile.exists()) {
            if (mpDbjFile.length() == 0)
                Log.d(TAG, "Error occurred attempting to restore DB");
            String dbString = null;
            try {
                dbString = new String(FileSystemUtils.read(mpDbjFile), "UTF-8");
            } catch (IOException e) {
                Log.e(TAG, "Failed to read survey file: " + FileName, e);
            }
            if (dbString != null) {
                ATSKTransferPackage fullDB = null;
                try {
                    fullDB = Conversions.fromJson(dbString,
                            /*AZProviderClient.legacyConversion(dbString),*/
                            ATSKTransferPackage.class);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read survey: " + FileName, e);
                }
                return fullDB;
            }
        }

        return null;
    }

    public void saveSurveyAsXML(String uid) {
        if (_mapView != null) {
            try {
                SurveyData survey = _azpc.getAZ(uid, false);
                if (survey != null) {
                    ODBGenerator odbGenerator = new ODBGenerator(
                            _mapView.getContext());
                    String file = getFileName(uid, "_odb.xml");
                    odbGenerator.GenerateAZODB(survey, file);
                    toast("Successfully exported survey to " + file);
                } else {
                    toast("Failed to load survey: " + uid);
                }
            } catch (IOException ie) {
                toast("error generating the odb file");
            } catch (javax.xml.transform.TransformerException te) {
                toast("error generating the odb file");
            } catch (javax.xml.parsers.ParserConfigurationException pce) {
                toast("error generating the odb file");
            }
        }
    }

    public void saveSurveyAsKML(String surveyUID) {
        if (_mapView != null) {

            String toastStr = "Export to KML failed.";

            //            final DefaultMapGroup collected = new DefaultMapGroup("Default");

            List<Folder> folderList = new ArrayList<Folder>(2);

            MapGroup azGrp = _mapView.getRootGroup().findMapGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_AZ);

            GeoBounds surveyBounds = null;

            if (azGrp != null) {

                MapGroup specificGrp = azGrp.findMapGroup(surveyUID);

                if (specificGrp != null) {
                    // Skip these since they might be invalid
                    // ends up creating a giant, incorrect focus area
                    final String[] ignoreUIDs = new String[] {
                            ATSKConstants.INCURSION_LINE_APPROACH,
                            ATSKConstants.INCURSION_LINE_APPROACH_WORST,
                            ATSKConstants.INCURSION_LINE_DEPARTURE,
                            ATSKConstants.INCURSION_LINE_DEPARTURE_WORST
                    };
                    double south = 90, west = 180, north = -90, east = -180;
                    for (MapItem mi : specificGrp.getItems()) {
                        if (mi instanceof Shape) {
                            String uid = mi.getUID();
                            boolean skip = false;
                            for (String s : ignoreUIDs) {
                                if (uid.endsWith(s)) {
                                    skip = true;
                                    break;
                                }
                            }
                            if (skip)
                                continue;
                            Shape shp = (Shape) mi;
                            GeoBounds bounds = shp.getBounds(null);
                            south = Math.min(bounds.getSouth(), south);
                            west = Math.min(bounds.getWest(), west);
                            north = Math.max(bounds.getNorth(), north);
                            east = Math.max(bounds.getEast(), east);
                        }
                    }
                    surveyBounds = new GeoBounds(south, west, north, east);
                    try {
                        Folder azFolder = KMLUtil.exportKMLMapGroup(
                                specificGrp, null);
                        if (azFolder != null)
                            folderList.add(azFolder);
                    } catch (FormatNotSupportedException fnse) {
                        Log.e(TAG, "Format Not Supported: azGroup");
                    }
                }
            }

            //GRADIENT GROUP NOT NEEDED
            //MapGroup gradGrp = mapView.getRootGroup().findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_GRD);

            MapGroup obsGrp = _mapView.getRootGroup().findMapGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_OBS);

            if (obsGrp != null) {

                MapGroup specificGrp = obsGrp.findMapGroup(surveyUID);

                //in case obstructions are put into a sub group like the az
                if (specificGrp == null)
                    specificGrp = obsGrp;

                Folder obsFolder;
                try {
                    obsFolder = KMLUtil.exportKMLMapGroup(specificGrp, null);
                    if (obsFolder != null) {

                        folderList.add(obsFolder);

                    }
                } catch (FormatNotSupportedException fnse) {
                    Log.e(TAG, "Format Not Supported: obsGrp");
                }

                try {
                    if (!folderList.isEmpty()) {
                        String file = writeKMLtoFile(folderList, surveyUID,
                                surveyBounds, "_AzObs");
                        toastStr = "Saved KML to " + file;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException: writeKMLtoFile");
                }

            }
            toast(toastStr);
        }
    }

    private String writeKMLtoFile(List<Folder> folders, String uid,
            GeoBounds surveyBounds, String typeStr)
            throws IOException {

        String filename = uid;
        List<Feature> features = new ArrayList<Feature>();
        List<StyleSelector> styleList = new ArrayList<StyleSelector>();
        for (int i = 0; i < folders.size(); i++) {
            Folder f = folders.get(i);
            List<Feature> fl = f.getFeatureList();
            if (fl != null && !fl.isEmpty())
                features.addAll(fl);

            List<StyleSelector> ls = f.getStyleSelector();
            if (ls != null && !ls.isEmpty())
                styleList.addAll(ls);

            if (i == 0)
                filename = f.getName();
        }

        Log.d(TAG, "Exporting top level features: " + features.size());
        Log.d(TAG, "Exporting styles: " + styleList.size());

        Kml kml = new Kml();
        Document document = new Document();
        kml.setFeature(document);
        document.setName(filename);
        document.setDescription(filename
                + " generated by "
                + ATAKConstants.getVersionName()
                +
                " on: "
                + KMLUtil.KMLDateTimeFormatter.get().format(
                        CoordinatedTime.currentDate()));
        document.setOpen(1);
        if (surveyBounds != null) {
            // Set focus point
            LookAt focusPoint = new LookAt();
            GeoPoint center = surveyBounds.getCenter(null);
            focusPoint.setLatitude(center.getLatitude());
            focusPoint.setLongitude(center.getLongitude());
            focusPoint.setAltitude(0.0);
            focusPoint.setRange(Conversions.CalculateRangem(
                    surveyBounds.getSouth(), surveyBounds.getWest(),
                    surveyBounds.getNorth(), surveyBounds.getEast()));
            focusPoint.setTilt(0.0);
            focusPoint.setHeading(0.0);
            document.setAbstractView(focusPoint);

            // Set region
            Region surveyRegion = new Region();
            LatLonAltBox box = new LatLonAltBox();
            box.setNorth(String.valueOf(surveyBounds.getNorth()));
            box.setSouth(String.valueOf(surveyBounds.getSouth()));
            box.setWest(String.valueOf(surveyBounds.getWest()));
            box.setEast(String.valueOf(surveyBounds.getEast()));
            surveyRegion.setLatLonAltBox(box);
            document.setRegion(surveyRegion);
        }
        document.setFeatureList(features);
        document.setStyleSelector(styleList);

        Serializer serializer = new Serializer();
        StringWriter sw = new StringWriter();
        try {
            serializer.write(kml, sw);
        } catch (Exception e) {
            throw new IOException(e);
        }

        String filepath = getFileName(uid, typeStr + ".kml");

        String data = sw.toString();

        writeFile(filepath, data);

        return filepath;

    }

    public void saveSurveyAsSHP(final String uid) {
        if (_mapView == null)
            return;
        String fileName = getFileName(uid, "-SHP.zip");
        final File outFile = new File(fileName);
        File parent = outFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            Log.w(TAG, "Failed to create directories: "
                    + parent.getAbsolutePath());
            toast("Failed to create directory " + parent);
            return;
        }
        SurveyData survey = _azpc.getAZ(uid, false);
        if (exportSurveySHP(survey, outFile)) {
            Log.d(TAG, "Successfully exported survey to " + outFile);
            toast("Exported survey to " + outFile.getAbsolutePath());
        } else {
            Log.d(TAG, "Failed to export survey to " + outFile);
            toast("Failed to save " + outFile.getAbsolutePath());
        }
    }

    private void toast(final String msg) {
        _mapView.post(new Runnable() {
            public void run() {
                Toast.makeText(_mapView.getContext(), msg, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    public void imageSurveyCapture() {

        if (_captureToolbar == null)
            _captureToolbar = new SurveyCaptureToolbar(_mapView);

        if (ImageCapture.isSaving()) {
            toast(_plugin.getString(R.string.save_busy));
            return;
        }

        Intent toolbar = new Intent(ToolbarBroadcastReceiver.OPEN_TOOLBAR);
        toolbar.putExtra("toolbar", SurveyCaptureToolbar.IDENTIFIER);
        AtakBroadcast.getInstance().sendBroadcast(toolbar);

        /*if (_mapView != null) {

            //close the dropdown
            DropDownManager.getInstance().hidePane();

            ArrayList<MapGroup> surveyMapGroups = new ArrayList<MapGroup>();

            MapGroup azGrp = _mapView.getRootGroup().findMapGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_AZ);

            //in case obstructions are put into a sub group like the az
            if (azGrp != null) {
                MapGroup specificGrp = azGrp.findMapGroup(uid);

                if (specificGrp != null) {
                    surveyMapGroups.add(specificGrp);
                }
            }

            //GRADIENT GROUP NOT NEEDED
            //            MapGroup gradGrp = mapView.getRootGroup().findMapGroup(ATSKATAKConstants.ATSK_MAP_GROUP_GRD);

            MapGroup obsGrp = _mapView.getRootGroup().findMapGroup(
                    ATSKATAKConstants.ATSK_MAP_GROUP_OBS);
            if (obsGrp != null) {
                MapGroup specificGrp = obsGrp.findMapGroup(uid);
                if (specificGrp != null) {
                    surveyMapGroups.add(specificGrp);
                } else {
                    surveyMapGroups.add(obsGrp);
                }
            }

            // Include range and bearing items
            MapGroup rabGroup = RangeAndBearingMapComponent.getGroup();
            if (rabGroup != null)
                surveyMapGroups.add(rabGroup);

            imgSurveyCapture(uid, surveyMapGroups);

        }*/
    }

    private void imgSurveyCapture(String uid, List<MapGroup> groups) {

        //        boolean didit = false;

        String file = getFileName(uid, "");

        File outFile = null;
        try {
            outFile = createFile(file);
        } catch (IOException ioe) {
            Log.e("ATSK", "Exception creating image export file: ", ioe);
        }

        if (outFile != null && outFile.exists()) {

            // Don't leave an empty file
            FileSystemUtils.deleteFile(outFile);

            final List<MapItem> items = new ArrayList<MapItem>();
            final List<MapItem> excludeditems = new ArrayList<MapItem>();

            for (MapGroup mg : groups) {//for each map group
                mg.deepForEachItem(new MapGroup.MapItemsCallback() {//for each map item
                    @Override
                    public boolean onItemFunction(MapItem mi) {

                        if (mi.getVisible()) {

                            //don't add LZ_INNER/OUTER_APPROACH/DEPARTURE
                            if (!(mi.getUID().endsWith(
                                    ATSKConstants.LZ_INNER_APPROACH)
                                    || mi.getUID().endsWith(
                                            ATSKConstants.LZ_INNER_DEPARTURE)
                                    || mi.getUID().endsWith(
                                            ATSKConstants.LZ_OUTER_APPROACH) || mi
                                    .getUID().endsWith(
                                            ATSKConstants.LZ_OUTER_DEPARTURE))) {
                                items.add(mi);
                            } else {
                                excludeditems.add(mi);
                            }

                        }
                        return false;//go through all of them
                    }
                });
            }

            //find the bounds and center
            GeoBounds bounds;
            ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();
            for (MapItem mi : items) {
                if (mi instanceof Shape) {
                    Collections.addAll(points, ((Shape) mi).getPoints());
                } else if (mi instanceof PointMapItem) {
                    points.add(((PointMapItem) mi).getPoint());
                }
            }
            GeoPoint[] thePoints = new GeoPoint[points.size()];
            points.toArray(thePoints);
            bounds = GeoBounds.createFromPoints(thePoints);

            items.addAll(excludeditems); //re-add the excluded items back in so they're drawn

            //register the layer
            //GLLayerFactory.register(GLManualSurveyImageCapture.SPI);

            // do the capture
            /*_msic = new ManualSurveyImageCapture(
                    _mapView, outFile, items, bounds);*/

        }
    }

    public boolean exportSurveySHP(SurveyData survey, File outFile) {

        MapGroup azGrp = _mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_AZ);
        if (azGrp == null || survey == null)
            return false;

        MapGroup surveyGrp = azGrp.findMapGroup(survey.uid);
        if (surveyGrp == null)
            return false;

        MapGroup obsGrp = _mapView.getRootGroup().findMapGroup(
                ATSKATAKConstants.ATSK_MAP_GROUP_OBS);

        List<MapItem> items = new ArrayList<MapItem>();
        items.addAll(surveyGrp.getItems());
        if (obsGrp != null) {
            // Only include obstructions within the 10km boundary
            double limit = (survey.circularAZ ? survey.getRadius() : Math.max(
                    survey.getFullWidth(), survey.getLength()))
                    + AZHelper.QUAD_OBSTRUCTION_LIMIT;
            double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
            for (int i = 0; i < 4; i++) {
                SurveyPoint sp = Conversions.AROffset(survey.center, i * 90,
                        limit);
                minLat = Math.min(minLat, sp.lat);
                maxLat = Math.max(maxLat, sp.lat);
                minLon = Math.min(minLon, sp.lon);
                maxLon = Math.max(maxLon, sp.lon);
            }
            FOVFilter filter = new FOVFilter(new GeoBounds(
                    minLat, minLon, maxLat, maxLon));
            for (MapItem item : obsGrp.getItems()) {
                if (filter.accept(item))
                    items.add(item);
            }
        }

        String surveyName = survey.getSurveyName();
        File pointsFile = new File(outFile.getParent(), surveyName + "-points");
        File arcFile = new File(outFile.getParent(), surveyName + "-lines");
        if (pointsFile.exists())
            FileSystemUtils.deleteFile(pointsFile);
        if (arcFile.exists())
            FileSystemUtils.deleteFile(arcFile);
        //File polyFile = new File(outFile.getParent(), surveyName + "-polygons");

        boolean ret = true;
        for (MapItem item : items) {
            if (!item.getVisible() || (item instanceof Marker
                    && ((Marker) item).getIconColor() == 0)
                    || item instanceof Shape
                    && ((Shape) item).getStrokeColor() == 0
                    || item instanceof RangeAndBearingEndpoint)
                continue;
            String itemName;
            if (item instanceof ATSKLineLeader)
                itemName = "";
            else if (item instanceof Arrow)
                itemName = ((Arrow) item).getText();
            else if (item instanceof ATSKShape)
                itemName = ((ATSKShape) item).getLabel();
            else
                itemName = HierarchyListAdapter.getTitleFromMapItem(
                        item, null, null, item.getGroup());
            if (itemName == null
                    || Double.compare(item.getMetaDouble("minRenderScale",
                            ATSKMarker.DEFAULT_MIN_RENDER_SCALE),
                            Double.MAX_VALUE) == 0
                    || item instanceof ATSKShape
                    && (((ATSKShape) item)
                            .getATSKType() == null || ((ATSKShape) item)
                            .getATSKType()
                            .equals(ATSKConstants.CURRENT_SCREEN_AZ)
                            && itemName.equals(surveyName)))
                itemName = "";

            boolean success = false;
            if (item instanceof Shape) {
                Shape shapeItem = (Shape) item;
                GeoPoint[] points = shapeItem.getPoints();
                if (points != null && points.length > 1) {
                    // TODO: Polygon type is buggy when imported back into ATAK
                    // <4 point polygons are invisible and segfault can occur when playing with zoom
                    /*if (points.length > 3 && (shapeItem.getStyle() & Polyline.STYLE_CLOSED_MASK) != 0)
                        success = ShapeFile.savePolygon(polyFile,
                                itemName, MapHelper.convertGeoPoints(points));*/
                    if ((shapeItem.getStyle() & Polyline.STYLE_CLOSED_MASK) != 0
                            && points[0] != points[points.length - 1]) {
                        // Add extra point for closed shapes
                        points = Arrays.copyOf(points, points.length + 1);
                        points[points.length - 1] = new GeoPoint(points[0]);
                    } else if (item instanceof Arrow && points.length == 2) {
                        // Include arrow as part of the shape
                        double range = 10, bearing = points[0]
                                .bearingTo(points[1]);
                        points = Arrays.copyOf(points, 5);
                        points[2] = DistanceCalculations
                                .computeDestinationPoint(
                                        points[1], bearing + 135, range);
                        points[3] = new GeoPoint(points[1]);
                        points[4] = DistanceCalculations
                                .computeDestinationPoint(
                                        points[1], bearing - 135, range);
                    }
                    success = ShapeFile.saveArc(arcFile,
                            itemName, MapHelper.convertGeoPoints(points));
                }
            } else if (item instanceof PointMapItem) {
                GeoPoint point = ((PointMapItem) item).getPoint();
                if (point != null)
                    success = ShapeFile.savePoint(pointsFile,
                            itemName,
                            MapHelper.convertGeoPoint2SurveyPoint(point));
            } else {
                // Export for item not supported, skip it
                Log.d(TAG, "Export for item type " + item.getClass()
                        + " not supported.");
                success = true;
            }
            if (!success) {
                Log.e(TAG, "Failed to save survey item " + item.getUID());
                ret = false;
            }
        }

        List<File> outFiles = new ArrayList<File>();
        File[] files = outFile.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if (fileName.startsWith(surveyName + "-"))
                    outFiles.add(f);
            }
        }

        // Zip output
        if (ret) {
            try {
                FileSystemUtils.zipDirectory(outFiles, outFile);
            } catch (Exception e) {
                Log.e(TAG, "Failed to zip shape files to " + outFile, e);
                ret = false;
            }
        }

        // Remove temp files
        for (File f : outFiles)
            FileSystemUtils.deleteFile(f);

        return ret;
    }
}
