package com.hitices.autopatrol.entity.imageData;

import android.os.Parcel;
import android.os.Parcelable;

import com.amap.api.maps2d.model.Marker;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by dusz7 on 20180426.
 */

public class AnalyzedImageBean implements Parcelable {

    public static final int IS_VISIBLE = 0;
    public static final int IS_INFRARED = 1;

    public static final int IS_NORMAL = 0;
    public static final int HAS_COVERED = 1;
    public static final int HAS_BROKEN = 2;

    private int imageType;

    private int imageState;

    private String imagePath;

    private Marker mapMarker;

    private List<MyRecognition> recognitions;

    public AnalyzedImageBean() {
        imagePath = "";
        imageType = IS_VISIBLE;
        imageState = IS_NORMAL;
        recognitions = new LinkedList<MyRecognition>();
    }

    public AnalyzedImageBean(String imagePath, int imageType) {
        this.imagePath = imagePath;
        this.imageType = imageType;
        imageState = IS_NORMAL;
        recognitions = new LinkedList<MyRecognition>();
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

    public List<MyRecognition> getRecognitions() {
        return recognitions;
    }

    public void setRecognitions(List<MyRecognition> recognitions) {
        this.recognitions = recognitions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(imagePath);
        parcel.writeInt(imageState);
        parcel.writeInt(imageType);
        parcel.writeList(recognitions);
    }

    public static final Parcelable.Creator<AnalyzedImageBean> CREATOR = new Parcelable.Creator<AnalyzedImageBean>() {
        @Override
        public AnalyzedImageBean createFromParcel(Parcel parcel) {
            AnalyzedImageBean image = new AnalyzedImageBean();
            image.imagePath = parcel.readString();
            image.imageState = parcel.readInt();
            image.imageType = parcel.readInt();
            image.recognitions = parcel.readArrayList(MyRecognition.class.getClassLoader());
            return image;
        }

        @Override
        public AnalyzedImageBean[] newArray(int i) {
            return new AnalyzedImageBean[i];
        }
    };
}
