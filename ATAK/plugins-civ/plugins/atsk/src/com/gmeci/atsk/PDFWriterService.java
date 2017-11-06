
package com.gmeci.atsk;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDocument;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate a PDF containing images
 */
public class PDFWriterService extends Service {

    private static final String TAG = "PDFWriterService";
    public static final String SERVICE_INTENT = "com.gmeci.atsk.PDFWriterService";
    public static final String FINISHED_INTENT = "com.gmeci.atsk.PDF_FINISHED";

    private List<File> _images;
    private List<Integer> _ori;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("pdf")) {
            File pdf = (File) intent.getSerializableExtra("pdf");
            _images = getListExtra(intent, "images", File.class);
            _ori = getListExtra(intent, "orientations", Integer.class);
            int result = Activity.RESULT_CANCELED;
            if (pdf != null && _images != null && _ori != null) {
                Log.d(TAG, "starting construction of the PDF file: " + pdf);
                if (generate(pdf)) {
                    Log.d(TAG, "successfully saved PDF: " + pdf);
                    result = Activity.RESULT_OK;
                } else
                    Log.e(TAG, "failed to save PDF: " + pdf);
            }
            Intent finished = new Intent(FINISHED_INTENT);
            finished.putExtra("resultCode", result);
            finished.putExtra("pdf", pdf);
            sendBroadcast(finished);
        }
        try {
            Log.d(TAG, "Returning onStartCommand(" + flags + ", " + startId
                    + ")");
            return super.onStartCommand(intent, flags, startId);
        } finally {
            Log.d(TAG, "Stopping service");
            stopSelf(startId);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");
    }

    /**
     * Generate PDF and add images as overlay content
     * We have to do this in 2 steps because adding images
     * the normal way doesn't work for some reason
     * @param pdf Final output PDF
     */
    public boolean generate(File pdf) {
        if (_images == null || _images.isEmpty())
            return false;
        PdfDocument doc = new PdfDocument();
        FileOutputStream fos = null;
        File tmp = new File(pdf.getParent(), "tmp-"
                + System.currentTimeMillis() + ".pdf");
        // Create PDF and add pages
        float margin = 0;
        boolean success = false;
        try {
            // Get max page size
            Rectangle ps = PageSize.LETTER;
            for (File imgFile : _images) {
                Rectangle ips = getPageSize(imgFile);
                ps = new Rectangle(Math.max(ps.getWidth(), ips.getWidth()),
                        Math.max(ps.getHeight(), ips.getHeight()));
            }
            Log.d(TAG,
                    "Calcualted max page size = " + ps.getWidth() + "x"
                            + ps.getHeight());
            margin = Math.max(ps.getWidth(), ps.getHeight()) * 0.03f;
            doc.setMargins(margin, margin, margin, margin);
            ps = new Rectangle(ps.getWidth() + margin * 2, ps.getHeight()
                    + margin * 2);
            doc.setPageSize(ps);
            fos = new FileOutputStream(tmp);
            doc.addWriter(PdfWriter.getInstance(doc, fos));
            doc.open();
            for (File imgFile : _images) {
                if (!imgFile.exists() || !imgFile.isFile())
                    continue;
                try {
                    doc.newPage();

                    // Add blank rectangle so the header gets created
                    Rectangle rect = new Rectangle(margin, margin,
                            ps.getWidth() - margin, ps.getHeight() - margin);
                    doc.add(rect);

                    // Normally this would suffice
                    //Image img = Image.getInstance(imgFile);
                    //doc.add(img);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create PDF image for " + imgFile, e);
                    success = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create temp PDF " + tmp, e);
            success = false;
        } finally {
            try {
                if (doc.isOpen())
                    doc.close();
            } catch (Exception e) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (Exception e) {
            }
        }
        // Then add the images
        try {
            addImages(tmp, pdf, margin);
            success = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to run manipulatePdf", e);
            success = false;
        } finally {
            if (!tmp.delete()) { 
                Log.e(TAG, "Failed to delete the tmp file");
            }
        }
        return success;
    }

    /**
     * Add images to temp document using a stamper
     * @param src Temp PDF
     * @param dest Final output
     * @throws IOException
     * @throws DocumentException
     */
    private void addImages(File src, File dest, float margin)
            throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src.getAbsolutePath());
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));

        int numPages = reader.getNumberOfPages();
        for (int i = 0; i < numPages && i < _images.size(); i++) {
            PdfContentByte cb = stamper.getOverContent(i + 1);
            if (cb == null) {
                Log.e(TAG, "Content for page " + (i + 1) + " is invalid");
                continue;
            }
            Image img;
            try {
                img = Image.getInstance(_images.get(i).getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Failed to read " + _images.get(i), e);
                img = null;
            }
            if (img == null)
                continue;
            if (img.getWidth() > img.getHeight()) {
                // Re-orient images to be readable in portrait and landscape viewing
                switch (i < _ori.size() ? _ori.get(i) : 0) {
                    default:
                    case 0:
                    case 90:
                        break;
                    case 180:
                    case 270:
                        img.setRotationDegrees(180);
                        break;
                }
            }
            // Fit to page and center
            Rectangle r = reader.getPageSize(i + 1);
            img.scaleToFit(r.getWidth() - margin * 2, r.getHeight() - margin
                    * 2);
            img.setAbsolutePosition((r.getWidth() - img.getScaledWidth()) / 2,
                    (r.getHeight() - img.getScaledHeight()) / 2);
            cb.addImage(img, true);
        }

        stamper.close();
        reader.close();
    }

    private Rectangle getPageSize(File img) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(img.getAbsolutePath(), opts);
        return new Rectangle(opts.outWidth, opts.outHeight);
    }

    /**
     * Get array list from intent serializable extra
     * Older versions of Android return Object[] instead of T[]
     * so we have to convert each element
     * @param intent The service intent
     * @param key The key for the array extra
     * @param cl The class of the list to return
     * @return List containing T or null if extra is not an array
     */
    private static <T> List<T> getListExtra(Intent intent, String key,
            Class<T> cl) {
        List<T> ret = null;
        Serializable extra = intent.getSerializableExtra(key);
        if (extra != null && extra instanceof Object[]) {
            ret = new ArrayList<T>();
            for (Object o : (Object[]) extra) {
                if (cl.isInstance(o))
                    ret.add(cl.cast(o));
            }
        }
        return ret;
    }
}
