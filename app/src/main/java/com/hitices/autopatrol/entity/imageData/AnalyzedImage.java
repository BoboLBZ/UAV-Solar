package com.hitices.autopatrol.entity.imageData;

import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.io.Serializable;

/**
 * Created by dusz7 on 20180426.
 */

public class AnalyzedImage implements Serializable{

    public static final int IS_VISIBLE = 0;
    public static final int IS_INFRARED = 1;

    public static final int IS_NORMAL = 0;
    public static final int HAS_COVERED = 1;
    public static final int HAS_BROKEN = 2;

    private int imageType;

    private int imageState;

    private String imagePath;

    private Marker mapMarker;

    public AnalyzedImage() {
        imagePath = "";
        imageType = IS_VISIBLE;
        imageState = IS_NORMAL;
    }

    public AnalyzedImage(String imagePath, int imageType) {
        this.imagePath = imagePath;
        this.imageType = imageType;
        imageState = IS_NORMAL;
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }

    public int getImageState() {
        return imageState;
    }

    public void setImageState(int imageState) {
        this.imageState = imageState;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Marker getMapMarker() {
        return mapMarker;
    }

    public void setMapMarker(Marker mapMarker) {
        this.mapMarker = mapMarker;
    }
}
