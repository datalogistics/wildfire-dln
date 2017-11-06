
package com.gmeci.atsk.gallery;

/**
 * Gallery image file reference for JSON export
 */
public class ATSKGalleryItem {
    public final String path;
    public final String surveyUID;

    public ATSKGalleryItem(String uid, String path) {
        surveyUID = uid;
        this.path = path;
    }
}
