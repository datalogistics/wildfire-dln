
package com.gmeci.atskservice.pdf;

import android.graphics.BitmapFactory;
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
import harmony.java.awt.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Generate a PDF containing images
 */
public class ImagePDFFiller {

    private static final String TAG = "ImagePDFFiller";

    private final List<File> _images;
    private final List<Integer> _ori;

    public ImagePDFFiller(List<File> images, List<Integer> orientations) {
        _images = images;
        _ori = orientations;
    }

    /**
     * Generate PDF and add images as overlay content
     * We have to do this in 2 steps because adding images
     * the normal way doesn't work for some reason
     * @param pdf Final output PDF
     */
    public void generate(File pdf) {
        if (_images.isEmpty())
            return;
        PdfDocument doc = new PdfDocument();
        FileOutputStream fos = null;
        File tmp = new File(pdf.getParent(), "tmp-"
                + System.currentTimeMillis() + ".pdf");
        // Create PDF and add pages
        float margin = 0;
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
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create temp PDF " + tmp, e);
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to run manipulatePdf", e);
        } finally {
            tmp.delete();
        }
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
}
