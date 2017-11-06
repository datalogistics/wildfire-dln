
package com.gmeci.atsk.gradient.graphs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import com.atakmap.coremap.log.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.SurveyData;
import com.gmeci.atskservice.resolvers.AZProviderClient;
import com.gmeci.atskservice.resolvers.GradientProviderClient;
import com.gmeci.helpers.GradientAnalysisOPCHelper;

import org.achartengine.GraphicalView;
import org.achartengine.chart.CombinedXYChart.XYCombinedChartDef;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.text.format.DateFormat;

import java.util.Comparator;
import java.util.Date;

public abstract class GradientGraphActivityBase {
    protected static final float LINE_WIDTH = 5f;
    protected static final float MAX_MIN_LINE_WIDTH = 2f;
    private static final String TAG = "GradientGraphActivity";
    protected XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    protected XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(
            2);
    protected GraphicalView mChart;
    protected Context _context;
    protected View view;
    protected AZProviderClient azpc;
    protected GradientProviderClient gpc;
    protected GradientAnalysisOPCHelper gaopc;
    protected int GraphsDrawn = 0;
    protected final ArrayList<XYCombinedChartDef> ListofLinesDrawn = new ArrayList<XYCombinedChartDef>();
    protected Button SnapButton;
    protected LinearLayout ChartSpot;
    protected TextView ChartTitleTV;
    protected String CurrentSurveyUID = "";
    protected SurveyData CurrentSurvey;

    protected DiagramSurfaceView mSurfView;

    protected String label = "NONE";

    protected boolean legacy;

    protected boolean[] actives;

    protected float scaleY = 1f;
    protected float maxScaleY = 30f;

    DecimalFormat df = new DecimalFormat("#0.0");

    public GradientGraphActivityBase(boolean legacy) {
        this.legacy = legacy;
    }

    protected void onDestroy() {
        azpc.Stop();
        gpc.Stop();
    }

    int GetGoodColor() {
        return 0xffffffff;
    }

    int GetBadColor() {
        return Color.argb(150, 240, 40, 40);
    }

    protected abstract void DisplayGradient(Context context);

    protected void setLabel(final String label) {
        this.label = label;
    }

    protected void init(final Context context) {
        _context = context;
        ChartTitleTV = (TextView) view.findViewById(R.id.Titlestatic);
        ChartSpot = (LinearLayout) view.findViewById(R.id.chart);

        azpc = new AZProviderClient(context);
        azpc.Start();

        gpc = new GradientProviderClient(context);
        gpc.Start();

        CurrentSurveyUID = azpc.getSetting(ATSKConstants.CURRENT_SURVEY, TAG);
        CurrentSurvey = azpc.getAZ(CurrentSurveyUID, false);
        Log.d(TAG, "currentSurvey: " + this.CurrentSurveyUID + " "
                + CurrentSurvey.toString());
        azpc.Stop();

        SnapButton = (Button) view.findViewById(R.id.snap);
        SnapButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveSnapshot();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save graph snapshot", e);
                    Toast.makeText(_context, "Failed to save graph snapshot.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public void refreshGraph() {
        if (mSurfView != null) {
            mSurfView.setActives(actives);
            mSurfView.setScaleY(scaleY);
            mSurfView.show();
        }
    }

    private void saveSnapshot() {
        Bitmap b;
        if (legacy) {
            int width = mChart.getWidth();
            int height = mChart.getHeight();
            b = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.drawARGB(255, 64, 64, 64);
            mChart.draw(c);
        } else
            b = mSurfView.getBackingBitmap();

        FileOutputStream out = null;

        final String fname = String.format("%s_%s_%s_%s.png",
                CurrentSurvey.getSurveyName(), CurrentSurvey.getType()
                        .toString(),
                DateFormat.format("yyyyMMdd'T'hhmm", new Date()), label);

        try {
            out = new FileOutputStream(getTarget(fname));
            if (b != null) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else {
                Log.e(TAG, "BACKING BITMAP NOT ESTABLISHED");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {

            try {
                if (out != null) {
                    out.close();
                    Toast.makeText(_context,
                            "Graph Saved to file: " + fname,
                            Toast.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (b != null && !b.isRecycled())
                b.recycle();
        }
    }

    protected File getTarget(String fName) {
        File dir = new File(ATSKConstants.ATSK_SURVEY_FOLDER_BASE
                + java.io.File.separator
                + CurrentSurvey.getType()
                + java.io.File.separator
                + CurrentSurvey.getSurveyName());
        if (!dir.exists() && !dir.mkdirs())
            Log.e(TAG, "Failed to make dir at " + dir.getAbsolutePath());
        return new File(dir, fName);
    }

    public static final Comparator<GradientDataPoint> LONGITUDINAL_SORT = new Comparator<GradientDataPoint>() {
        @Override
        public int compare(GradientDataPoint lhs, GradientDataPoint rhs) {
            return Double.compare(lhs.getX(), rhs.getX());
        }
    };

    public static class GradientDataPoint {
        private double x;
        private double y;

        public GradientDataPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void setY(double newy) {
            y = newy;
        }

        public void setX(double newx) {
            x = newx;
        }
    }

}
