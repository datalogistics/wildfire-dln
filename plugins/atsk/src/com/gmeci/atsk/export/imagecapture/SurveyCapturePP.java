
package com.gmeci.atsk.export.imagecapture;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;

import com.atakmap.android.imagecapture.CanvasHelper;
import com.atakmap.android.imagecapture.CaptureDialog;
import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.imagecapture.GLCaptureTool;
import com.atakmap.android.imagecapture.PointA;
import com.atakmap.android.imagecapture.TextRect;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.maps.MapItem;
import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MGRSPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.math.MathUtils;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.visibility.VizController;
import com.gmeci.atsk.visibility.VizPrefs;
import com.gmeci.conversions.Conversions;
import com.gmeci.conversions.Conversions.Unit;
import com.gmeci.core.ATSKConstants;
import com.gmeci.core.LineObstruction;
import com.gmeci.core.SurveyData;
import com.gmeci.core.SurveyPoint;
import com.gmeci.helpers.AZHelper;
import com.gmeci.helpers.LineHelper;
import com.gmeci.helpers.PolygonHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.atakmap.coremap.locale.LocaleUtil;
import java.util.Map;

/**
 * Class for handling post-processing on base imagery
 * such as drawing map items, labels, shapes, etc.
 */
class SurveyCapturePP extends CapturePP {

    private final Context _plugin;
    private final SurveyData _survey;
    private final Map<String, PointA> _surveyPoints = new HashMap<String, PointA>();
    private static final Path _northStar = new Path();

    private static final String[] SURVEY_LABELS = {
            ATSKConstants.HLZ_APPROACH,
            ATSKConstants.HLZ_DEPARTURE,
            ATSKConstants.DZ_HEADING,
            ATSKConstants.SURVEY_WIDTH,
            ATSKConstants.SURVEY_LENGTH
    };

    static {
        // Build star shape
        float[] v = new float[] {
                0.809017f, 0.58778524f,
                0.5f, -0.3632713f,
                1.309017f, -0.95105654f,
                0.309017f, -0.95105654f,
                0f, -1.9021131f,
                -0.309017f, -0.95105654f,
                -1.309017f, -0.95105654f,
                -0.5f, -0.3632713f,
                -0.809017f, 0.58778524f,
                0, 0
        };
        for (int i = 0; i < v.length; i += 2)
            _northStar.lineTo(v[i], v[i + 1]);
        _northStar.close();
    }

    SurveyCapturePP(GLCaptureTool capture, SurveyData survey) {
        super(capture, 1);
        _survey = survey;
        if (_survey != null) {
            _title = _survey.getSurveyName();
            MGRSPoint mgrs = MGRSPoint.fromLatLng(Ellipsoid.WGS_84,
                    _survey.center.lat, _survey.center.lon, null);
            _location = mgrs.getFormattedString().replace("  ", " ");
        }
        _plugin = ATSKApplication.getInstance().getPluginContext();

        // Recalculate map scale since survey is no longer null
        _mapRange = SpanUtilities.convert(_horizRange, Span.METER,
                getDisplayUnit());
        _mapScale = (int) Math.pow(10, Math.floor(Math.log10(_mapRange)));
        _scaleSeg = _mapScale / 5.0;
    }

    @Override
    public synchronized Span getDisplayUnit() {
        return _survey != null && _survey.getType()
                    == SurveyData.AZ_TYPE.DZ ? Span.YARD : Span.FOOT;
    }

    @Override
    public synchronized CaptureDialog getCaptureDialog() {
        return new SurveyCaptureDialog(_mapView, _survey, this);
    }

    /**
     * Set the 3 fields to be shown in the info box
     * By default these values are obtained during setup()
     * @param surveyName Survey name
     * @param dateStamp Date of capture
     * @param location Location of survey
     */
    @Override
    public synchronized void setInfo(
            String surveyName, String location, String dateStamp) {
        _title = surveyName;
        _dateStamp = dateStamp;
        _location = location;
    }

    private PointA forward(SurveyPoint sp) {
        return new PointA(forward(new GeoPoint(sp.lat, sp.lon)), 0);
    }

    private PointA forward(LineObstruction lo) {
        if (lo == null || lo.points == null)
            return null;
        List<SurveyPoint> points = new ArrayList<SurveyPoint>(lo.points);
        if (points.size() >= 2)
            points.add(0, new SurveyPoint(
                    (points.get(0).lat + points.get(1).lat) / 2,
                    (points.get(0).lon + points.get(1).lon) / 2));
        for (SurveyPoint sp : points) {
            PointF p = forward(sp);
            if (inside(p))
                return new PointA(p, 0);
        }
        return null;
    }

    @Override
    public synchronized void calcForwardPositions() {

        // Make all items visible we can toggle them later
        VizController cont = new VizController(_mapView, _survey);
        boolean[] viz = new boolean[VizPrefs.CB_COUNT];
        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            viz[i] = VizPrefs.get(i);
        cont.setAllVisible(true);

        _items.clear();
        _items.addAll(_subject.getItems(_bounds));

        super.calcForwardPositions();

        for (int i = 0; i < VizPrefs.CB_COUNT; i++)
            VizPrefs.set(i, viz[i]);
        cont.syncPrefs();

        if (_survey == null || _survey.getType() == SurveyData.AZ_TYPE.FARP)
            return;

        // Survey labels
        if (_survey.circularAZ)
            _surveyPoints.put(ATSKConstants.SURVEY_WIDTH,
                    forward(AZHelper.CalculateCenterOfEdge(0,
                            _survey.getRadius() * 2,
                            _survey.center, false)));
        else {
            PointA widthPoint = forward(AZHelper
                    .CalculateCenterOfEdge(_survey, false));
            PointA lenPoint = forward(
                    AZHelper.CalculateCenterOfEdge(_survey.angle - 90,
                            _survey.width, _survey.center,
                            _survey.angle > 90 && _survey.angle <= 270));
            widthPoint.angle = (float) _survey.angle;
            lenPoint.angle = (float) _survey.angle + 90;
            _surveyPoints.put(ATSKConstants.SURVEY_WIDTH, widthPoint);
            _surveyPoints.put(ATSKConstants.SURVEY_LENGTH, lenPoint);
        }
        if (_survey.getType() == SurveyData.AZ_TYPE.HLZ) {
            if (VizPrefs.get(VizPrefs.HLZ_APPROACH_LINE)) {
                LineObstruction appLine = PolygonHelper
                        .getHLZApproachLine(_survey);
                _surveyPoints.put(ATSKConstants.HLZ_APPROACH,
                        forward(appLine));
            }
            if (VizPrefs.get(VizPrefs.HLZ_DEPARTURE_LINE)) {
                LineObstruction depLine = PolygonHelper
                        .getHLZDepartureLine(_survey);
                _surveyPoints.put(ATSKConstants.HLZ_DEPARTURE,
                        forward(depLine));
            }
        } else if (_survey.getType() == SurveyData.AZ_TYPE.DZ
                && !_survey.circularAZ
                && VizPrefs.get(VizPrefs.DZ_HEADING_LINE)) {
            LineObstruction headLine = PolygonHelper.getDZHeadingLine(_survey);
            _surveyPoints
                    .put(ATSKConstants.DZ_HEADING, forward(headLine));
        }
    }

    @Override
    protected synchronized void drawMapItem(MapItem mi) {
        if (mi.getVisible())
            super.drawMapItem(mi);
    }

    /**
     * Overlay declination and survey info onto captured bitmap
     */
    @Override
    public synchronized void drawOverlays() {
        if (_survey == null)
            return;

        Resources res = _plugin.getResources();
        int width = _can.getWidth(), height = _can.getHeight();
        float margin = dp(5), padding = dp(4);
        GeoPoint center = _bounds.getCenter(null);
        boolean drawArrows = VizPrefs.get(VizPrefs.IMGCAP_ARROWS), drawInfoBox = VizPrefs
                .get(VizPrefs.IMGCAP_INFO), drawScale = VizPrefs
                .get(VizPrefs.IMGCAP_SCALE);

        // Survey dimensions
        Unit unit = getDisplayUnit() == Span.YARD ? Unit.YARD : Unit.FOOT;
        if (VizPrefs.get(VizPrefs.IMGCAP_DIMENSIONS)) {
            for (String key : SURVEY_LABELS) {
                PointA pos = _surveyPoints.get(key);
                if (pos == null || !inside(pos))
                    continue;
                String txt;
                if (key.equals(ATSKConstants.SURVEY_WIDTH)) {
                    if (_survey.getType() == SurveyData.AZ_TYPE.LZ)
                        continue;
                    if (_survey.circularAZ)
                        txt = String
                                .format(LocaleUtil.getCurrent(),
                                        "Radius: %.0f %s",
                                        Unit.METER.convertTo(
                                                _survey.getRadius(), unit),
                                        unit.getAbbr());
                    else
                        txt = String.format(LocaleUtil.getCurrent(), "%.0f %s",
                                Unit.METER.convertTo(_survey.width, unit),
                                unit.getAbbr());
                } else if (key.equals(ATSKConstants.SURVEY_LENGTH)) {
                    if (_survey.circularAZ)
                        continue;
                    if (_survey.getType() == SurveyData.AZ_TYPE.LZ)
                        txt = String.format(LocaleUtil.getCurrent(),
                                "%.0f X %.0f %s",
                                Unit.METER.convertTo(_survey.getLength(false),
                                        unit),
                                Unit.METER.convertTo(_survey.width, unit),
                                unit.getAbbr());
                    else
                        txt = String
                                .format(LocaleUtil.getCurrent(),
                                        "%.0f %s",
                                        Unit.METER.convertTo(
                                                _survey.getLength(), unit),
                                        unit.getAbbr());
                } else
                    continue;
                drawLabel(txt, pos);
            }
        }

        // Survey labels
        if (VizPrefs.get(VizPrefs.IMGCAP_HEADINGS)
                && (_survey.getType() == SurveyData.AZ_TYPE.HLZ
                || _survey.getType() == SurveyData.AZ_TYPE.DZ)) {

            for (String key : SURVEY_LABELS) {
                PointF loc = _surveyPoints.get(key);
                if (loc == null)
                    continue;
                double ang, awayAng;
                int color;
                if (key.equals(ATSKConstants.HLZ_APPROACH)) {
                    ang = _survey.approachAngle;
                    awayAng = ang - 90;
                } else if (key.equals(ATSKConstants.HLZ_DEPARTURE)) {
                    ang = _survey.departureAngle;
                    awayAng = ang + 90;
                } else if (key.equals(ATSKConstants.DZ_HEADING)) {
                    ang = _survey.angle;
                    awayAng = ang + 90;
                } else {
                    continue;
                }
                // Lighten color up a bit
                float[] hsv = new float[3];
                Color.colorToHSV(VizPrefs.getColor(key,
                        LineHelper.getLineColor(key)), hsv);
                hsv[1] = 0.5f;
                color = Color.HSVToColor(hsv);
                _paint.setColor(color);
                _paint.setTextSize(_labelSize);

                double mag = Conversions.GetMagAngle(ang,
                        _survey.center.lat, _survey.center.lon);
                String hText = String.format(
                        res.getString(R.string.atsk_deg_hlz), mag, 'M');

                TextRect hBox = new TextRect(_paint, padding, hText);
                float boxW = hBox.width(), boxH = hBox.height();

                // Scale position at full res
                loc = new PointF(dr(loc.x), dr(loc.y));

                // DEBUG: Draw text location as a box
                //can.drawRect(loc.x-10, loc.y-10, loc.x+10, loc.y+10, paint);

                // Move away from line
                loc = CanvasHelper.degOffset(loc, awayAng,
                        margin + boxW / 2.0f, margin + boxH / 2.0f);

                // Keep text within image bounds
                hBox.offsetTo(
                        MathUtils.clamp(loc.x - boxW / 2, 0, width - boxW),
                        MathUtils.clamp(loc.y - boxH / 2, 0, height - boxH));

                hBox.draw(_can, dp(2));
            }
        }

        resetPaint();
        if (drawArrows) {
            // True/declination arrows
            _paint.setTextAlign(Paint.Align.CENTER);
            GeomagneticField gmf = new GeomagneticField(
                    (float) center.getLatitude(),
                    (float) center.getLongitude(),
                    0, CoordinatedTime.currentDate().getTime());
            double declination = gmf.getDeclination();
            if (declination > 180)
                declination -= 360;

            // instead of rendering the exact mag arrow, render an artistic representation.
            float artisticSep = (declination < 0 ? -25 : 25);

            // Arrow specs
            _paint.setTextSize(_fontSize * 0.75f);
            float trueLen = dp(40), magLen = trueLen * 0.8f, tipLen = dp(6);
            PointF arrows = new PointF(0, trueLen + _fontSize / 2 + margin);

            // Mag arrow
            PointF magTip = CanvasHelper.degOffset(arrows, artisticSep, magLen);
            float magMiterLen = magLen + tipLen * 0.75f;
            PointF miterMagTip = CanvasHelper.degOffset(arrows, artisticSep,
                    magMiterLen);

            // Magnetic north text box
            String magString = String.format(
                    res.getString(R.string.atsk_deg_north2),
                    Math.abs(declination))
                    + (declination < 0 ? "W" : "E");
            TextRect magBox = new TextRect(_paint, _pdFontSize, magString);
            float magOffset = Math.abs(miterMagTip.x - arrows.x);
            int compassPos = (Integer) SurveyCapturePrefs.get(
                    SurveyCapturePrefs.PREF_COMPASS_POS);
            if (compassPos == SurveyCapturePrefs.COMPASS_POS_LEFT) {
                float delta = Math.max(magOffset, margin + (declination < 0
                        ? magBox.width() + magOffset : 0));
                arrows.x += delta;
                magTip.x += delta;
                miterMagTip.x += delta;
            } else if (compassPos == SurveyCapturePrefs.COMPASS_POS_RIGHT) {
                float delta = Math.min(width - magOffset, width - margin -
                        (declination > 0 ? magOffset + magBox.width() : 0));
                arrows.x += delta;
                magTip.x += delta;
                miterMagTip.x += delta;
            }
            magBox.setPos(new PointF(miterMagTip.x, magTip.y),
                    (declination < 0 ?
                            TextRect.ALIGN_RIGHT : TextRect.ALIGN_LEFT)
                            | TextRect.ALIGN_TOP);

            _paint.setStyle(Paint.Style.FILL);
            _paint.setColor(Color.argb(64, 0, 0, 0));
            _can.drawRoundRect(magBox, _pdFontSize, _pdFontSize, _paint);

            _paint.setColor(Color.WHITE);
            magBox.draw(_can, _borderWidth);

            // Draw dotted arc between mag and true arrows
            _path.reset();
            float arcOffset = magLen + tipLen * 0.25f;
            _path.arcTo(new RectF(arrows.x - arcOffset, arrows.y - arcOffset,
                    arrows.x + arcOffset, arrows.y + arcOffset),
                    artisticSep - 90,
                    -artisticSep, true);
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setPathEffect(new DashPathEffect(new float[] {
                    dp(0.1f), dp(0.75f)
            }, 0));
            _paint.setStrokeWidth(_borderWidth * 0.5f);
            CanvasHelper.drawPathBorder(_can, _path, _borderWidth * 0.5f,
                    _paint);
            _paint.setPathEffect(null);
            _paint.setStrokeWidth(_borderWidth);

            // Magnetic north arrow
            _path.reset();
            _path.moveTo(arrows.x, arrows.y);
            _path.lineTo(magTip.x, magTip.y);
            PointF headTail = CanvasHelper.degOffset(magTip, artisticSep
                    + 160 * Math.signum(artisticSep), tipLen);
            _path.lineTo(headTail.x, headTail.y);
            PointF endHead = CanvasHelper.degOffset(arrows, artisticSep,
                    magLen - (tipLen / 2));
            _path.lineTo(endHead.x, endHead.y);

            // True north
            PointF trueTip = new PointF(arrows.x, arrows.y - trueLen);
            CanvasHelper.lineToPoint(_path, arrows, trueTip);

            // North star
            Matrix starMat = new Matrix();
            starMat.postScale(_borderWidth, _borderWidth);
            starMat.postTranslate(trueTip.x, trueTip.y);
            _path.addPath(_northStar, starMat);
            _paint.setStrokeJoin(Paint.Join.MITER);
            float miter = _paint.getStrokeMiter();
            _paint.setStrokeMiter(miter * 2);
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setColor(Color.WHITE);
            CanvasHelper.drawPathBorder(_can, _path, _borderWidth,
                    _paint);
            _paint.setStrokeMiter(miter);

            // Draw "MN"
            _paint.setStrokeJoin(Paint.Join.ROUND);
            _paint.setStyle(Paint.Style.FILL);
            _paint.setTextSize(_fontSize * 0.5f);
            TextRect mn = new TextRect(_paint, _pdFontSize, "MN");
            mn.alignTo(TextRect.ALIGN_X_CENTER | TextRect.ALIGN_BOTTOM);
            _can.save();
            _can.translate(miterMagTip.x, miterMagTip.y);
            _can.rotate(artisticSep);
            mn.draw(_can, _borderWidth);
            _can.restore();
        }

        float fullBarHeight = 0;
        if (drawScale) {
            // Scale bar calculations
            float barLen = (float) (width * (_mapScale / _mapRange));
            float barSegLen = (float) (width * (_scaleSeg / _mapRange));
            int segs = (int) Math.ceil(barLen / barSegLen);
            String scaleText = String.format(LocaleUtil.getCurrent(),
                    "%.0f %s", _mapScale, getDisplayUnit().getAbbrev());

            // Scale bar lines
            resetPaint();
            _paint.setColor(Color.BLACK);
            float barHeight = dp(3);
            float barY = height - margin - barHeight;
            CanvasHelper.drawRectBorder(_can, _paint, _borderWidth,
                    new RectF(margin, barY, margin + barLen, barY + barHeight));

            // Draw main segments
            for (int i = 0; i < segs; i++) {
                float segX = margin + barSegLen * i;
                _paint.setColor(i % 2 == 0 ? Color.WHITE : Color.RED);
                _can.drawRect(segX, barY,
                        segX + Math.min(barLen - (barSegLen * i),
                                barSegLen), barY + barHeight, _paint);
            }

            // Scale text w/ box
            _paint.setColor(Color.WHITE);
            _paint.setStyle(Paint.Style.FILL);
            _paint.setTextAlign(Paint.Align.CENTER);
            _paint.setTextSize(_fontSize * 0.75f);
            TextRect scaleBox = new TextRect(_paint, _pdFontSize * 0.75f,
                    scaleText);
            scaleBox.offsetTo(margin + barLen / 2 - scaleBox.width() / 2,
                    barY - scaleBox.height() - _borderWidth);
            CanvasHelper.drawRectBorder(_can, _paint, _borderWidth, scaleBox);
            _paint.setColor(Color.BLACK);
            scaleBox.draw(_can);
            fullBarHeight = scaleBox.height() + barHeight + _pdFontSize * 4;
        }

        resetPaint();
        if (drawInfoBox) {
            // Info box
            int infoPos = (Integer) SurveyCapturePrefs.get(
                    SurveyCapturePrefs.PREF_INFOBOX_POS);
            _paint.setTextAlign(Paint.Align.LEFT);
            List<String> lines = new ArrayList<String>();
            if (!FileSystemUtils.isEmpty(_title))
                lines.add(_title);
            if (!FileSystemUtils.isEmpty(_location))
                lines.add(_location);
            if (!FileSystemUtils.isEmpty(_dateStamp))
                lines.add(_dateStamp);
            TextRect infoBox = new TextRect(_paint, padding,
                    lines.toArray(new String[lines.size()]));
            infoBox.setTypeface(0, Typeface.DEFAULT_BOLD);
            float maxY = height - infoBox.height() - margin - fullBarHeight;
            float maxX = width - infoBox.width() - margin;
            switch (infoPos) {
                default:
                case SurveyCapturePrefs.INFO_POS_TR:
                    // Top right
                    infoBox.offsetTo(maxX, margin);
                    break;
                case SurveyCapturePrefs.INFO_POS_TL:
                    // Top left
                    infoBox.offsetTo(margin, margin);
                    break;
                case SurveyCapturePrefs.INFO_POS_BR:
                    // Bottom right
                    infoBox.offsetTo(maxX, maxY);
                    break;
                case SurveyCapturePrefs.INFO_POS_BL:
                    // Bottom left
                    infoBox.offsetTo(margin, maxY);
                    break;
            }

            _paint.setStyle(Paint.Style.FILL);
            _paint.setColor(Color.WHITE);
            CanvasHelper.drawRectBorder(_can, _paint, _borderWidth, infoBox);
            _paint.setColor(Color.BLACK);
            infoBox.draw(_can);
        }
    }
}
