
package com.gmeci.atsk.gallery;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.atsk.plugin.R;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.image.ImageContainer;
import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.filesystem.HashingUtils;
import com.gmeci.atsk.map.ATSKMapManager;
import com.gmeci.atsk.resources.ATSKApplication;
import com.gmeci.atsk.resources.ThumbView;

import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * List of survey images based on file path
 */
public class ATSKGalleryAdapter extends BaseAdapter {
    private static final String TAG = "ATSKGalleryAdapter";
    private static final long WEEK = 1000 * 60 * 60 * 24 * 7;
    private static final int THUMB_SIZE = 300;
    private final List<File> _files;
    private final List<String> _selected;
    private boolean _multiSelect = false;
    private String _surveyUID;
    private final MapView _mapView;
    private final FragmentManager _fragManager;
    private final Resources _res;
    private final Drawable _drLoading, _drInvalid;

    private final List<String> _genCache = new ArrayList<String>();
    private final Map<String, Bitmap> _thumbCache = new HashMap<String, Bitmap>();

    public ATSKGalleryAdapter(MapView mapView, FragmentManager fragManager) {
        // Only add image files
        _files = new ArrayList<File>();
        refresh();
        _selected = new ArrayList<String>();
        _mapView = mapView;
        _fragManager = fragManager;

        _res = ATSKApplication.getInstance().getPluginContext().getResources();
        _drLoading = _res.getDrawable(R.drawable.loading);
        _drInvalid = _res.getDrawable(R.drawable.atsk_delete);
    }

    public synchronized void setSurveyUID(String uid) {
        _surveyUID = uid;
    }

    public void refresh() {
        final File[] imgs;
        synchronized (this) {
            imgs = ATSKGalleryUtils.getImages(_surveyUID);
        }
        dispose();
        synchronized (_files) {
            _files.clear();
            for (File f : imgs) {
                if (isImage(f))
                    _files.add(f);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        synchronized (_files) {
            return _files.size();
        }
    }

    @Override
    public Object getItem(int position) {
        synchronized (_files) {
            return _files.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void dispose() {
        synchronized (_thumbCache) {
            if (_thumbCache.isEmpty())
                return;
            long released = 0;
            for (Bitmap bmp : _thumbCache.values()) {
                if (bmp != null && !bmp.isRecycled()) {
                    released += bmp.getByteCount();
                    recycleBmp(bmp);
                }
            }
            //Log.d(TAG, "Released " + released + " bytes from " + _thumbCache.size() + " thumbnails.");
            _thumbCache.clear();
        }
    }

    public String[] getSelected() {
        if (FileSystemUtils.isEmpty(_selected))
            return new String[] {};

        String[] f = new String[_selected.size()];
        return _selected.toArray(f);
    }

    public void enableMultiSelect(boolean enabled) {
        _multiSelect = enabled;
        _selected.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder {
        // Displays image thumbnail
        ThumbView imageView;
        // Full-image file
        File file;
        // MIME type
        TextView textType;
        // File name
        TextView textFilename;
        // Multi-selection check box
        CheckBox chkSelect;
        RelativeLayout chkSelectLayout;
        // Container for mime type and file name
        LinearLayout imageLayout;
        // Prevents excessive reloading of thumbnails
        boolean loading;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            // Create new gallery item
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            convertView = inf.inflate(R.layout.image_gallery_item, null);
            holder = new ViewHolder();
            holder.imageView = (ThumbView) convertView
                    .findViewById(R.id.gallery_img_view);
            holder.chkSelectLayout = (RelativeLayout) convertView
                    .findViewById(R.id.gallery_img_select);
            holder.chkSelect = (CheckBox) convertView
                    .findViewById(R.id.gallery_img_checkbox);

            holder.imageLayout = (LinearLayout) convertView
                    .findViewById(R.id.gallery_img_layout);
            holder.textType = (TextView) convertView
                    .findViewById(R.id.gallery_img_type);
            holder.textFilename = (TextView) convertView
                    .findViewById(R.id.gallery_img_name);
            holder.loading = true;

            convertView.setTag(holder);
        } else {
            // Use existing gallery item
            holder = (ViewHolder) convertView.getTag();
        }

        // Invalid item position
        if (position >= getCount()) {
            Log.w(TAG, "Invalid position: " + position);
            holder.imageView.setImageDrawable(_drInvalid);
            holder.textType.setText("");
            holder.textFilename.setText("");
            return convertView;
        }

        // Get file mime type
        final File img;
        synchronized (_files) {
            img = _files.get(position);
        }
        final ResourceFile.MIMEType t =
                ResourceFile.getMIMETypeForFile(img.getName());

        if (t != null) {
            //display type
            holder.textType.setText(t.name());
        } else {
            //display file extension
            holder.textType.setText(FileSystemUtils.getExtension(img, true,
                    true));
        }
        holder.file = img;
        holder.imageView.setAdapter(this);

        // Tick check box if file is selected
        holder.chkSelectLayout.setVisibility(_multiSelect ? CheckBox.VISIBLE
                : CheckBox.GONE);
        holder.chkSelect.setOnCheckedChangeListener(null);
        if (_multiSelect && !FileSystemUtils.isEmpty(_selected))
            holder.chkSelect.setChecked(_selected.contains(img
                    .getAbsolutePath()));
        else
            holder.chkSelect.setChecked(false);

        if (_multiSelect) {
            //tap on parent constitutes a checkbox click
            holder.imageView.post(new Runnable() {
                public void run() {
                    Rect outRect = new Rect();
                    holder.imageView.getHitRect(outRect);
                    holder.imageView.setTouchDelegate(new TouchDelegate(
                            outRect, holder.chkSelect));
                }
            });
        } else {
            holder.imageView.setTouchDelegate(null);
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    viewImage(holder.file);
                }
            });
            holder.imageView
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            viewMarkup(holder.file);
                            return true;
                        }
                    });
        }

        holder.chkSelect.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton,
                            boolean bSelected) {
                        String path = holder.file.getAbsolutePath();
                        if (bSelected) {
                            if (!_selected.contains(path))
                                _selected.add(path);
                        } else {
                            if (_selected.contains(path))
                                _selected.remove(path);
                        }
                    }
                });

        // Only attempt to load files with image extensions
        if (isImage(holder.file)) {
            if (holder.loading) {
                holder.imageView.setImageDrawable(_drLoading);
                holder.textFilename.setText(R.string.loading);
            }
            // Load image thumbnail
            load.execute(new Runnable() {
                public void run() {
                    genThumbnail(holder.file);
                    if (getFromThumbCache(holder.file) == null) {
                        if (generating(holder.file))
                            return;
                        holder.imageView.post(new Runnable() {
                            public void run() {
                                if (holder.file == img) {
                                    holder.imageView
                                            .setImageDrawable(_drInvalid);
                                    holder.textFilename.setText(img.getName());
                                }
                            }
                        });
                    } else {
                        holder.imageView.post(new Runnable() {
                            public void run() {
                                if (holder.file == img) {
                                    synchronized (_thumbCache) {
                                        holder.imageView.setImageBitmap(
                                                getFromThumbCache(img));
                                    }
                                    holder.textFilename.setText(img.getName());
                                } else {
                                    synchronized (_thumbCache) {
                                        Bitmap image = getFromThumbCache(img);
                                        recycleBmp(image);
                                        _thumbCache.remove(holder.file
                                                .getAbsolutePath());
                                    }
                                }
                            }
                        });
                    }
                    holder.loading = false;
                }
            });
        } else {
            //otherwise see if there's an icon to use
            if (t == null) {
                //last resort display a generic icon
                Log.d(TAG, "No supported icon: " + position);
                holder.imageView.setImageDrawable(_drInvalid);
            } else {
                //use icon
                ATAKUtilities.SetIcon(_mapView.getContext(), holder.imageView,
                        t.ICON_URI, Color.WHITE);
            }
        }
        return convertView;
    }

    Thread cacheReaper = new Thread() {
        public void run() {
            final File cache = ATSKGalleryUtils.getImageCache();
            if (!cache.exists() || !cache.isDirectory())
                return;

            File[] files = cache.listFiles();

            if (files != null) {
                for (File f : files) {
                    long mod = f.lastModified();
                    if (Math.abs(mod - System.currentTimeMillis()) > WEEK) {
                        //Log.d(TAG, "scheduling for delete: " + f);
                        FileSystemUtils.deleteFile(f);
                    }
                }
            }
        }
    };

    /**
     * Hack to update the lastModified time.
     */
    public void setLastModified(final File f) throws IOException {
        if (f == null || !f.exists())
            return;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            long length = raf.length();
            raf.setLength(length + 1);
            raf.setLength(length);
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void viewImage(File file) {
        if (isImage(file)) {
            Log.d(TAG, "Viewing image: " + file.getAbsolutePath());

            // Find matching marker
            MapItem item = null;
            synchronized (this) {
                if (_surveyUID != null)
                    item = ATSKMapManager.find(_surveyUID
                            + "_pic_" + file.getName());
            }

            if (item == null) {
                // Fallback to creating new marker
                Log.w(TAG, "Failed to find existing gallery marker.");
                GeoPoint gp = ExifHelper.fixImage(_mapView,
                        file.getAbsolutePath());
                if (gp != null && gp.isValid()) {
                    // Create temporary marker
                    Marker picMarker = new PlacePointTool.MarkerCreator(gp)
                            .setUid(UUID.randomUUID().toString())
                            .setType("b-i-x-i")
                            .showCotDetails(false)
                            .setNeverPersist(true)
                            .setCallsign(file.getName())
                            .placePoint();
                    picMarker.setTouchable(false);
                    picMarker.setMetaString("tempGalleryMarker", "true");
                    item = picMarker;
                }
            }

            // Request image container drop down
            final String[] imageURIs;
            synchronized (_files) {
                imageURIs = new String[_files.size()];
                int i = 0;
                for (File f : _files)
                    imageURIs[i++] = Uri.fromFile(f).toString();
            }
            Intent intent = new Intent(ImageDropDownReceiver.IMAGE_DISPLAY)
                    .putExtra("imageURI", Uri.fromFile(file).toString())
                    .putExtra("imageURIs", imageURIs);
            if (item != null) {
                intent.putExtra("uid", item.getUID());
                MapTouchController.goTo(item, false);
            }
            AtakBroadcast.getInstance().sendBroadcast(intent);
        }
    }

    private void viewMarkup(File file) {
        if (isImage(file)) {
            if (ATSKMarkup.upToDate(file)) {
                toast("Image already contains markup.");
                viewImage(file);
            } else {
                ATSKMarkupDialog dlg = new ATSKMarkupDialog();
                dlg.setupImage(file);
                dlg.show(_fragManager, TAG);
            }
        }
    }

    private boolean generating(File f) {
        synchronized (_genCache) {
            return _genCache.contains(f.getAbsolutePath());
        }
    }

    private void setGenerating(File f, boolean gen) {
        synchronized (_genCache) {
            String path = f.getAbsolutePath();
            if (gen && !_genCache.contains(path))
                _genCache.add(path);
            else
                _genCache.remove(path);
        }
    }

    private void toast(String msg) {
        Toast.makeText(_mapView.getContext(),
                msg, Toast.LENGTH_SHORT).show();
    }

    private Bitmap rotateBitmap(Bitmap bmp, int rot) {
        switch (rot) {
            case ExifInterface.ORIENTATION_ROTATE_180:
                rot = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rot = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rot = 90;
                break;
        }
        if (rot != 0) {
            // Get rotation matrix
            Matrix matrix = new Matrix();
            matrix.postRotate(rot);

            // Create new rotated bitmap
            Bitmap ret = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                    bmp.getHeight(), matrix, false);
            recycleBmp(bmp);
            return ret;
        }
        return bmp;
    }

    private boolean isImage(File file) {
        return file.isFile() && ImageDropDownReceiver
                .ImageFileFilter.accept(null, file.getName());
    }

    private int procid = 0;
    private final Object lock = new Object();
    final Object[] proclock = new Object[] {
            new Object(), new Object(), new Object(),
            new Object(), new Object(), new Object()
    };

    public static File getCacheFilepath(final File source,
            final File destFolder, final String destExt) {
        //final long start = System.currentTimeMillis();
        String baseName = HashingUtils.md5sum(source.getAbsolutePath()
                + source.lastModified());
        //Log.d(TAG, "hash lookup: " + baseName + " time: " + (System.currentTimeMillis() - start));
        return new File(destFolder, baseName + "." + destExt);
    }

    private void genThumbnail(final File f) {
        String path = f.getAbsolutePath();
        synchronized (_thumbCache) {
            // Stop if thumbnail already exists
            Bitmap thumb = _thumbCache.get(path);
            if (thumb != null && !thumb.isRecycled())
                return;
        }

        // Prevent unnecessary loading of the same thumbnail
        if (generating(f))
            return;
        setGenerating(f, true);

        final File cacheDir = ATSKGalleryUtils.getImageCache();
        if (!cacheDir.exists() && !cacheDir.mkdir())
            Log.d(TAG, "Cache directory failed to be created.");

        synchronized (lock) {
            if (cacheReaper != null) {
                cacheReaper.start();
                cacheReaper = null;
            }
        }

        synchronized (proclock[(procid++) % proclock.length]) {
            if (procid > 30000)
                procid = 0; //

            final File cacheFile = getCacheFilepath(f, cacheDir, "png");
            Bitmap source = null;

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferQualityOverSpeed = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;

                if (cacheFile.exists()) {
                    //Log.d(TAG, "cache hit: " + cacheFile);
                    final Bitmap retval =
                            BitmapFactory.decodeFile(cacheFile
                                    .getAbsolutePath(), opts);

                    if (retval != null) {
                        try {
                            setLastModified(cacheFile);
                        } catch (IOException e) {
                            Log.d(TAG, "unable to set the last modified time:"
                                    + cacheFile);
                        }
                        // valid cache file
                        addToThumbCache(path, retval);
                        setGenerating(f, false);
                        return;
                    }

                    if (!cacheFile.delete()) {
                        Log.d(TAG, "unable to delete " + cacheFile);
                    } else {
                        Log.d(TAG, "invalid cache file, deleting: " + cacheFile);
                    }
                }

                Log.w(TAG, "cache miss for \"" + f.getName() + "\": "
                        + cacheFile);

                long startTime = System.currentTimeMillis();
                if (ImageContainer.NITF_FilenameFilter.accept(
                        f.getParentFile(), f.getName())) {
                    source = ImageContainer
                            .readNITF(f, THUMB_SIZE * THUMB_SIZE);
                } else {
                    // Decode as little data as possible
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
                    opts.inJustDecodeBounds = false;
                    if (opts.outWidth > THUMB_SIZE
                            && opts.outHeight > THUMB_SIZE) {
                        opts.inSampleSize = 1 << (int) (Math.floor(log2(Math
                                .min(opts.outWidth, opts.outHeight))
                                - log2(THUMB_SIZE)));
                    }
                    source = BitmapFactory
                            .decodeFile(f.getAbsolutePath(), opts);
                }

                Bitmap retval =
                        ThumbnailUtils.extractThumbnail(
                                source,
                                THUMB_SIZE,
                                THUMB_SIZE);

                // Release source now that we're not using it
                if (source != null) {
                    recycleBmp(source);
                    source = null;
                }
                Log.d(TAG, "Took " + (System.currentTimeMillis() - startTime)
                        + "ms to generate thumbnail for " + f.getName());

                TiffImageMetadata exif = ExifHelper.getExifMetadata(f);
                // Note: Image caption is stored in "ImageDescription" exif tag
                // Referenced by TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION
                final String imageCaption = ExifHelper.getString(exif,
                        TiffConstants.TIFF_TAG_IMAGE_DESCRIPTION, null);
                final int rot = ExifHelper.getInt(exif,
                        TiffConstants.EXIF_TAG_ORIENTATION, 0);

                PrintWriter printWriter = null;
                try {
                    if (!FileSystemUtils.isEmpty(imageCaption)) {
                        String exifCache = cacheFile.getAbsolutePath()
                                .replace(".png", ".exif");
                        printWriter = new PrintWriter(exifCache,
                                FileSystemUtils.UTF8_CHARSET);
                        printWriter.println(imageCaption);
                    }
                } catch (UnsupportedEncodingException uee) {
                    Log.d(TAG, "Failed to save image metadata.", uee);
                } catch (IOException ioe) {
                    Log.d(TAG, "unable to extract metadata: " + cacheFile, ioe);
                } finally {
                    if (printWriter != null)
                        printWriter.close();
                }

                // Rotate thumbnail based on EXIF orientation
                final Bitmap thumb = rotateBitmap(retval, rot);

                if (thumb != null) {
                    save.execute(new Runnable() {
                        public void run() {
                            FileOutputStream fos = null;
                            try {
                                synchronized (thumb) {
                                    fos = new FileOutputStream(
                                            cacheFile);
                                    thumb.compress(Bitmap.CompressFormat.PNG,
                                            100,
                                            fos);
                                    Log.d(TAG, "Saved \"" + f.getName()
                                            + "\" cache file to "
                                            + cacheFile.getName());
                                }
                            } catch (IOException e) {
                                Log.d(TAG, "unable to save:" + cacheFile);
                                if (!cacheFile.delete())
                                    Log.d(TAG, "unable to delete:" + cacheFile);
                            } catch (IllegalStateException e) {
                                Log.d(TAG, "unable to save (recycled):"
                                        + cacheFile);
                                if (!cacheFile.delete())
                                    Log.d(TAG, "unable to delete: " + cacheFile);
                            } finally {
                                try {
                                    if (fos != null)
                                        fos.close();
                                } catch (IOException e) {
                                }
                                setGenerating(f, false);
                            }
                        }
                    });
                } else
                    setGenerating(f, false);
                addToThumbCache(path, thumb);
                return;
            } catch (Exception e) {
                boolean b = cacheFile.delete();
                if (!b)
                    Log.d(TAG, "error, deleting the cache entry for: " + f);
            } catch (OutOfMemoryError e) {
                Log.w(TAG, "Ran out of memory getting thumbnail for " + f);
            } finally {
                recycleBmp(source);
            }
            setGenerating(f, false);
        }
    }

    private void addToThumbCache(String path, Bitmap bmp) {
        synchronized (_thumbCache) {
            Bitmap existing = _thumbCache.get(path);
            if (existing != null && !existing.isRecycled())
                existing.recycle();
            _thumbCache.put(path, bmp);
        }
    }

    private Bitmap getFromThumbCache(File f) {
        synchronized (_thumbCache) {
            return _thumbCache.get(f.getAbsolutePath());
        }
    }

    private static void recycleBmp(Bitmap bmp) {
        if (bmp != null && !bmp.isRecycled())
            bmp.recycle();
    }

    private static double log2(double val) {
        return Math.log(val) / Math.log(2);
    }

    private final ExecutorService save = Executors.newFixedThreadPool(5,
            new NamedThreadFactory(
                    "ImageCachePool"));
    private final ExecutorService load = Executors.newFixedThreadPool(5,
            new NamedThreadFactory(
                    "ImageLoadPool"));

    static private class NamedThreadFactory implements ThreadFactory {
        int count = 0;
        final String name;

        public NamedThreadFactory(final String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" + count++);
        }
    }
}
