package com.hitices.autopatrol.entity.imageData;

import android.os.Parcel;
import android.os.Parcelable;

import com.amap.api.maps2d.model.Marker;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by dusz7 on 20180426.
 */

public class RecognizingImageBean implements Parcelable {

    public static final int IS_VISIBLE = 0;
    public static final int IS_INFRARED = 1;

    public static final int IS_NORMAL = 0;
    public static final int HAS_COVERED = 1;
    public static final int HAS_BROKEN = 2;

    private int imageType;

    private int solarState;

    private String imagePath;

    private Marker mapMarker;

    private List<MyRecognition> recognitions;

    public RecognizingImageBean() {
        imagePath = "";
        imageType = IS_VISIBLE;
        solarState = IS_NORMAL;
        recognitions = new LinkedList<MyRecognition>();
    }

    public RecognizingImageBean(String imagePath, int imageType) {
        this.imagePath = imagePath;
        this.imageType = imageType;
        solarState = IS_NORMAL;
        recognitions = new LinkedList<MyRecognition>();
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }

    public int getSolarState() {
        return solarState;
    }

    public void setSolarState(int solarState) {
        this.solarState = solarState;
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
        parcel.writeInt(solarState);
        parcel.writeInt(imageType);
        parcel.writeList(recognitions);
    }

    public static final Parcelable.Creator<RecognizingImageBean> CREATOR = new Parcelable.Creator<RecognizingImageBean>() {
        @Override
        public RecognizingImageBean createFromParcel(Parcel parcel) {
            RecognizingImageBean image = new RecognizingImageBean();
            image.imagePath = parcel.readString();
            image.solarState = parcel.readInt();
            image.imageType = parcel.readInt();
            image.recognitions = parcel.readArrayList(MyRecognition.class.getClassLoader());
            return image;
        }

        @Override
        public RecognizingImageBean[] newArray(int i) {
            return new RecognizingImageBean[i];
        }
    };
}
