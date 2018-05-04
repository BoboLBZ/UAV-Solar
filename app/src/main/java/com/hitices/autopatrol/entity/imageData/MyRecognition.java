package com.hitices.autopatrol.entity.imageData;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import com.hitices.autopatrol.tfObjectDetection.Classifier;

/**
 * Created by dusz7 on 20180427.
 */

public class MyRecognition implements Parcelable {

    private String title;
    private float confidence;
    private RectF location;

    private MyRecognition() {

    }

    public MyRecognition(Classifier.Recognition recognition) {
        title = recognition.getTitle();
        confidence = recognition.getConfidence();
        location = recognition.getLocation();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public RectF getLocation() {
        return location;
    }

    public void setLocation(RectF location) {
        this.location = location;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeFloat(confidence);
        parcel.writeParcelable(location, i);
    }

    public static final Parcelable.Creator<MyRecognition> CREATOR = new Parcelable.Creator<MyRecognition>() {
        @Override
        public MyRecognition createFromParcel(Parcel parcel) {
            MyRecognition myRecognition = new MyRecognition();
            myRecognition.title = parcel.readString();
            myRecognition.confidence = parcel.readFloat();
            myRecognition.location = parcel.readParcelable(RectF.class.getClassLoader());
            return myRecognition;
        }

        @Override
        public MyRecognition[] newArray(int i) {
            return new MyRecognition[i];
        }
    };
}
