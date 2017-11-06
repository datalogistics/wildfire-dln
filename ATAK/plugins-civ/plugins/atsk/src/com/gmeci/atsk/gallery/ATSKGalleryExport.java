
package com.gmeci.atsk.gallery;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.attachment.export.AttachmentExportMarshal;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.gmeci.atsk.PDFWriterService;
import com.gmeci.atsk.toolbar.ATSKToolbarComponent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ATSKGalleryExport {

    public static final String TAG = "ATSKGalleryExport";

    private final List<File> _files;
    private final File _outDir;
    private final boolean _markup;
    private final File _pdf;
    private ProgressDialog _prog;
    private final MapView _mapView;
    private boolean _canceled = false;

    public ATSKGalleryExport(MapView mapView, File[] files, File outDir,
            boolean markup, File pdf) {
        _mapView = mapView;
        _files = Arrays.asList(files);
        _outDir = outDir;
        _markup = markup;
        _pdf = pdf;
    }

    public ATSKGalleryExport(MapView mapView, File[] files, boolean zip,
            boolean markup, File pdf) {
        File none = ATSKGalleryUtils.getTempImage(new File("temp.zip"));
        _mapView = mapView;
        _files = Arrays.asList(files);
        _outDir = (zip ? none : none.getParentFile());
        _markup = markup;
        _pdf = pdf;
    }

    public void start(final Callback callback) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            createProgress();
            // Export shouldn't run on main thread
            new Thread(new Runnable() {
                public void run() {
                    start(callback);
                }
            }).start();
        } else {
            _mapView.post(new Runnable() {
                public void run() {
                    createProgress();
                }
            });
            export();
            if (callback != null)
                callback.onFinish(_files);
        }
    }

    public void start() {
        start(null);
    }

    private void createProgress() {
        if (_prog == null) {
            _prog = new ProgressDialog(_mapView.getContext());
            _prog.setTitle("Export Images");
            _prog.setMessage(_outDir.isDirectory() ? "Exporting images..."
                    : "Preparing images for export...");
            _prog.setMax(_files.size());
            _prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _prog.setCancelable(true);
            _prog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    _canceled = true;
                }
            });
            _prog.setProgress(0);
            _prog.show();
        }
    }

    private void export() {
        final List<Exportable> exports = new ArrayList<Exportable>();
        int fileNum = 1;
        final boolean zip = !_outDir.isDirectory();
        File[] outFiles = new File[_files.size()];
        for (File f : _files) {
            boolean copied = false;
            if (_markup && !ATSKMarkup.upToDate(f)) {
                // Apply markup first
                File out = ATSKMarkup.getOutputFile(f);
                if (zip)
                    out = ATSKGalleryUtils.getTempImage(out);
                else
                    out = new File(_outDir, out.getName());
                if (ATSKMarkup.applyMarkup(f, out, _markupProg) != null) {
                    f = out;
                    Log.d(TAG, "Successfully marked up " + out);
                    copied = true;
                }
            }
            if (!zip && !copied && !f.getParent()
                    .equals(_outDir.getAbsolutePath())) {
                try {
                    FileSystemUtils.copyFile(f, new File(_outDir, f.getName()));
                } catch (IOException e) {
                }
            }
            if (zip)
                exports.add(new AttachmentExportMarshal.FileExportable(f));
            outFiles[fileNum - 1] = f;
            final int progValue = fileNum++;
            _mapView.post(new Runnable() {
                public void run() {
                    if (_prog != null)
                        _prog.setProgress(progValue);
                }
            });
            if (_canceled) {
                toast("Canceled gallery export.");
                return;
            }
        }

        if (zip && FileSystemUtils.isEmpty(exports) || _files.size() == 0) {
            Log.w(TAG, "Cannot export empty list");
            return;
        }

        _mapView.post(new Runnable() {
            public void run() {
                if (_prog != null)
                    _prog.hide();
                Log.d(TAG, "Exporting file count: " + _files.size());
                if (zip) {
                    try {
                        AttachmentExportMarshal aem = new AttachmentExportMarshal(
                                _mapView.getContext());
                        aem.execute(exports);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to export", e);
                    }
                }
            }
        });
        exportToPDF(_pdf, outFiles);
    }

    public static void exportToPDF(File pdf, File... imgs) {
        if (pdf == null || imgs == null)
            return;
        if (pdf.exists())
            FileSystemUtils.deleteFile(pdf);
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<Integer> ori = new ArrayList<Integer>();
        for (File f : imgs) {
            if (!FileSystemUtils.isFile(f) || !f.isFile())
                continue;
            files.add(f);
            ori.add(ExifHelper.getImageOrientation(f));
        }
        if (files.isEmpty())
            return;

        if (_savePDFs.isEmpty())
            AtakBroadcast.getInstance().registerSystemReceiver(_pdfReceiver,
                    new AtakBroadcast.DocumentedIntentFilter(
                            PDFWriterService.FINISHED_INTENT));
        _savePDFs.add(pdf);

        Intent pdfIntent = new Intent(PDFWriterService.SERVICE_INTENT);
        pdfIntent.putExtra("pdf", pdf);
        pdfIntent.putExtra("images", files.toArray(new File[files.size()]));
        pdfIntent
                .putExtra("orientations", ori.toArray(new Integer[ori.size()]));
        MapView.getMapView().getContext().startService(pdfIntent);

    }

    private final static Set<File> _savePDFs = new HashSet<File>();
    private final static BroadcastReceiver _pdfReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode",
                    Activity.RESULT_CANCELED);
            File pdf = (File) intent.getSerializableExtra("pdf");
            if (pdf != null && _savePDFs.contains(pdf)) {
                Context plugin = ATSKToolbarComponent.getToolbar()
                        .getPluginContext();
                if (resultCode == Activity.RESULT_OK)
                    toast(String.format(LocaleUtil.getCurrent(), plugin
                            .getString(R.string.pdf_success), pdf));
                else
                    toast(plugin.getString(R.string.pdf_fail));
                _savePDFs.remove(pdf);
                if (_savePDFs.isEmpty())
                    AtakBroadcast.getInstance().unregisterSystemReceiver(
                            _pdfReceiver);
            }
        }
    };

    private static void toast(final String str) {
        final MapView mv = MapView.getMapView();
        if (mv == null)
            return;
        mv.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mv.getContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    public interface Callback {
        void onFinish(List<File> exported);
    }

    private final ATSKMarkup.ProgressCallback _markupProg = new ATSKMarkup.ProgressCallback() {
        @Override
        public boolean onProgress(int prog, int max) {
            // Stop if the user canceled
            return !_canceled;
        }
    };
}
