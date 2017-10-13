
package com.gmeci.vehicle;

import android.graphics.PointF;
import android.os.Environment;
import android.util.Log;

import com.gmeci.conversions.Conversions;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyPoint;
import com.gmeci.csvparser.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VehicleBlock {
    public static final String TAG = "VehicleBlock";
    public static final String BLOCK_PATH =
            Environment.getExternalStorageDirectory() + File.separator
                    + "atsk" + File.separator + "ac_block_files";

    public static final int TYPE_OTHER = -1;
    public static final int TYPE_HELO = 0;
    public static final int TYPE_FWAC = 1;

    private static final HashMap<File, VehicleBlock> _blockCache = new HashMap<File, VehicleBlock>();

    // Instance fields
    private final File _file;
    private ArrayList<PointF> _points;
    private double _length, _width, _height;
    private int _type = TYPE_OTHER;
    private boolean _valid = false;

    // Helo aircraft only
    private double _contingency, _training, _brownOut, _rotorClearance;

    // Fixed-wing aircraft only
    private double _clearanceAC, _clearanceTX,
            _setbackPA, _setbackTX;

    public VehicleBlock(File blockFile) {
        _file = blockFile;
        _points = new ArrayList<PointF>();
        if (!_blockCache.containsKey(_file)) {
            reload();
            _blockCache.put(_file, this);
        } else
            loadFromCache();
    }

    public VehicleBlock(String blockName) {
        this(VehicleBlock.getBlockFile(blockName));
    }

    /**
     * Get vehicle dimensions as array
     * @return Length, width, and height in meters
     */
    public double[] getDimensions() {
        return new double[] {
                _length, _width, _height
        };
    }

    /**
     * Vehicle block validity
     * @return True if vehicle block was loaded successfully
     */
    public boolean isValid() {
        return _valid;
    }

    /**
     * Get vehicle radials as array
     * @return Associated vehicle radii in meters
     */
    public double[] getRadials() {
        if (_type == TYPE_HELO)
            return new double[] {
                    _rotorClearance, _contingency,
                    _training, _brownOut
            };
        else if (_type == TYPE_FWAC)
            return new double[] {
                    _clearanceAC, _clearanceTX,
                    _setbackPA, _setbackTX
            };
        return new double[0];
    }

    public int getType() {
        return _type;
    }

    private void loadFromCache() {
        VehicleBlock existing = _blockCache.get(_file);
        if (existing != null) {
            _valid = existing._valid;
            _points = existing._points;
            _length = existing._length;
            _width = existing._width;
            _height = existing._height;
            _type = existing._type;
            if (_type == TYPE_HELO) {
                _rotorClearance = existing._rotorClearance;
                _contingency = existing._contingency;
                _training = existing._training;
                _brownOut = existing._brownOut;
            } else if (_type == TYPE_FWAC) {
                _clearanceAC = existing._clearanceAC;
                _clearanceTX = existing._clearanceTX;
                _setbackPA = existing._setbackPA;
                _setbackTX = existing._setbackTX;
            }
        }
    }

    /**
     * Reload vehicle block information from file
     */
    private void reload() {
        if (_file == null)
            return;
        if (!_file.exists()) {
            Log.e(TAG, "Vehicle block " + _file + " does not exist.");
            return;
        }
        _type = TYPE_OTHER;
        _points.clear();
        CSVReader csv = null;
        try {
            InputStreamReader isr = new InputStreamReader(
                    new FileInputStream(_file),
                    Charset.forName("UTF-8"));
            csv = new CSVReader(isr);
            //Skip the header
            csv.readNext();
            String[] currentRow;
            while ((currentRow = csv.readNext()) != null) {
                if (currentRow[0].startsWith("#"))
                    continue;
                // Type is defined 1 line below the header
                if (currentRow[0].equals("HELO"))
                    _type = TYPE_HELO;
                else if (currentRow[0].equals("LTFW"))
                    _type = TYPE_FWAC;
                switch (currentRow.length) {
                    case 2:
                        // XY point offsets
                        _points.add(new PointF(Float.parseFloat(currentRow[0]),
                                Float.parseFloat(currentRow[1])));
                        break;
                    case 3:
                        // Dimensions
                        _length = Double.parseDouble(currentRow[0]);
                        _width = Double.parseDouble(currentRow[1]);
                        _height = Double.parseDouble(currentRow[2]);
                        break;
                    case 4:
                        if (_type == TYPE_HELO) {
                            // Helo radii
                            _training = Double.parseDouble(currentRow[0]);
                            _contingency = Double.parseDouble(currentRow[1]);
                            _brownOut = Double.parseDouble(currentRow[2]);
                            _rotorClearance = Double.parseDouble(currentRow[3]);
                        } else if (_type == TYPE_FWAC) {
                            _clearanceAC = Double.parseDouble(currentRow[0]);
                            _clearanceTX = Double.parseDouble(currentRow[1]);
                            _setbackPA = Double.parseDouble(currentRow[2]);
                            _setbackTX = Double.parseDouble(currentRow[3]);
                        }
                        break;
                }
            }
            _valid = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read block " + _file.getAbsolutePath(), e);
        } finally {
            try {
                if (csv != null)
                    csv.close();
            } catch (IOException e) {
            }
        }
    }

    /* STATIC FIELDS */
    public static File getBlockDir() {
        return new File(BLOCK_PATH);
    }

    /**
     * Get block file based on name
     * @param name Block name
     * @return Block file (from default block directory)
     */
    public static File getBlockFile(String name) {
        return new File(BLOCK_PATH, name + ".block");
    }

    /**
     * Get block name based on file
     * @param block Block file
     * @return Block name
     */
    public static String getBlockName(File block) {
        String name = block.getName();
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * Return list of blocks within default block directory
     * @return List of block names
     */
    public static String[] getBlocks() {
        File dir = getBlockDir();
        if (dir.exists() && dir.isDirectory()) {
            File[] blockFiles = dir.listFiles();
            String[] blocks;
            if (blockFiles != null) {
                blocks = new String[blockFiles.length];
                for (int i = 0; i < blockFiles.length; i++) {
                    String name = blockFiles[i].getName();
                    if (name.endsWith(".block"))
                        blocks[i] = getBlockName(blockFiles[i]);
                }
                return blocks;
            }
        }
        return new String[0];
    }

    /**
     * Get vehicle block associated with file
     * @param blockFile Block file
     * @return Vehicle block object
     */
    public static VehicleBlock getBlock(File blockFile) {
        if (_blockCache.containsKey(blockFile))
            return _blockCache.get(blockFile);
        return new VehicleBlock(blockFile);
    }

    /**
     * Get vehicle block associated with name
     * @param name Block name
     * @return Vehicle block object
     */
    public static VehicleBlock getBlock(String name) {
        return getBlock(getBlockFile(name));
    }

    public static ArrayList<PointF> getBlockPoints(File blockFile) {
        return getBlock(blockFile)._points;
    }

    public static ArrayList<PointF> getBlockPoints(String name) {
        return getBlockPoints(getBlockFile(name));
    }

    public static double[] getBlockDimensions(String name) {
        return getBlock(name).getDimensions();
    }

    // For helicopters: { contingency, training, brown-out }
    // For planes/jets: { ac clearance, apron clearance,
    //          taxiway clearance, parking setback, taxiway setback }
    public static double[] getBlockRadials(String name) {
        return getBlock(name).getRadials();
    }

    public static List<SurveyPoint> buildPolyline(String name,
            SurveyPoint center) {
        return buildPolyline(getBlockFile(name), center);
    }

    public static List<SurveyPoint> buildPolyline(File blockFile,
            SurveyPoint center) {
        List<SurveyPoint> ret = new ArrayList<SurveyPoint>();
        List<PointF> points = getBlockPoints(blockFile);

        boolean closed = true;
        if (points.size() > 1) {
            PointF first = points.get(0), last = points.get(points.size() - 1);
            closed = first.equals(last.x, last.y);
        }

        // Offset points by average
        PointF avg = new PointF(0, 0);
        int pSize = points.size();
        if (closed)
            pSize--;
        double div_size = 1d / pSize;
        for (int i = 0; i < pSize; i++) {
            avg.x += points.get(i).x * div_size;
            avg.y += points.get(i).y * div_size;
        }

        // Convert to geopoints
        for (PointF p : points) {
            PointF nextPoint = new PointF(p.x - avg.x, p.y - avg.y);
            double[] offset = Conversions.AROffset(center.lat, center.lon,
                    center.course_true + 180, nextPoint.y);
            offset = Conversions.AROffset(offset[0], offset[1],
                    center.course_true + 270, nextPoint.x);
            ret.add(new SurveyPoint(offset[0], offset[1], center
                    .getHAEAltitude()));
        }

        // Close shape if we need to
        if (!closed)
            ret.add(ret.get(0));

        return ret;
    }

    public static LineObstruction buildLineObstruction(String name,
            SurveyPoint center) {
        LineObstruction lo = new LineObstruction();
        lo.points.addAll(buildPolyline(name, center));
        lo.closed = true;
        lo.filled = true;
        lo.width = 1;
        lo.height = getBlockDimensions(name)[2];
        return lo;
    }
}
