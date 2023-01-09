package com.openalpr.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

public class AlprDetection {

    private Bitmap mImage;
    private String mPlate;
    private double mConfidence;
    private String mRegion;
    private double mRegionConfidence;
    private int mX0;
    private int mY0;
    private int mX1;
    private int mY1;
    private double mProcessTime;
    private RectF m_location;

    public AlprDetection(byte[] image, String plate, double confidence, int x0, int y0, int x1, int y1, String region, double regionConfidence, double processTime) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        mImage = bitmap;
        mPlate = plate;
        mConfidence = confidence;
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mRegion = region;
        mRegionConfidence = regionConfidence;
        mProcessTime = processTime;
        m_location = new RectF();
        m_location.left = x0;
        m_location.top = y0;
        m_location.right = x1;
        m_location.bottom = y1;
    }

    public String getmPlate() {
        return mPlate;
    }

    public double getmConfidence() {
        return mConfidence;
    }

    public int getmX0() {
        return mX0;
    }

    public int getmY0() {
        return mY0;
    }

    public int getmX1() {
        return mX1;
    }

    public int getmY1() {
        return mY1;
    }

    public String getmRegion() {
        return mRegion;
    }

    public double getmRegionConfidence() {
        return mRegionConfidence;
    }

    public double getmProcessTime() {
        return mProcessTime;
    }


    public Bitmap getmImage() {
        return mImage;
    }

    public Bitmap getCropImage() {
        int w_width = getmX1() - getmX0();
        int w_height = getmY1() - getmY0();
        Bitmap w_detectionBitmap = Bitmap.createBitmap(mImage, getmX0(), getmY0(), w_width, w_height);
        return w_detectionBitmap;
    }

    public RectF getmLocation() {
        return m_location;
    }
}
