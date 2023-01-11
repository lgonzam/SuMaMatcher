package es.suma.matmatcher.ui;
/*
 * 2022 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
 * Based on TensorFlow sample,
 * from https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection
 * sources: BoxTracker.java
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * SumaMatcher App is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * version 3 as published by the Free Software Foundation
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;


import es.suma.matmatcher.util.FileUtils;
import es.suma.matmatcher.util.Logger;
import es.suma.matmatcher.model.AlprDetection;

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 */
public class BoxTracker {

    private final Logger logger = new Logger();
    private final Paint boxPaint = new Paint();
    private final Paint cropPaint = new Paint();
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;
    private String label;
    private double confidence;
    private RectF detectionScreenRect;
    private AlprDetection detection;
    private Bitmap rgbFrameBitmap;
    private RectF shapeScreenRect;
    private boolean initDraw=false;
    private float m_textSize=30;

    public BoxTracker() {
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);
    }

    public BoxTracker(float textSize) {
        m_textSize=textSize;
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);
    }

    public synchronized void setFrameConfiguration(
            final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }


    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    public synchronized void draw(final Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier =
                Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));

        frameToCanvasMatrix =
                FileUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);

        if ( shapeScreenRect!=null){

            final RectF shapePos = new RectF(shapeScreenRect);
            shapePos.top += 5;
            shapePos.left += 5;
            shapePos.bottom -= 5;
            shapePos.right -= 5;

            cropPaint.setColor(Color.BLUE);
            cropPaint.setStyle(Paint.Style.STROKE);
            cropPaint.setStrokeWidth(10);

            canvas.drawLine(shapePos.left, shapePos.top, shapePos.left+40, shapePos.top, cropPaint);
            canvas.drawLine(shapePos.left, shapePos.top, shapePos.left, shapePos.top+40, cropPaint);

            canvas.drawLine(shapePos.right-40, shapePos.top, shapePos.right, shapePos.top, cropPaint);
            canvas.drawLine(shapePos.right, shapePos.top, shapePos.right, shapePos.top+40, cropPaint);

            canvas.drawLine(shapePos.right-40, shapePos.bottom, shapePos.right, shapePos.bottom, cropPaint);
            canvas.drawLine(shapePos.right, shapePos.bottom-40, shapePos.right, shapePos.bottom, cropPaint);

            canvas.drawLine(shapePos.left, shapePos.bottom, shapePos.left+40, shapePos.bottom, cropPaint);
            canvas.drawLine(shapePos.left, shapePos.bottom-40, shapePos.left, shapePos.bottom, cropPaint);

        }

        initDraw=true;

        if ( detection==null){
            return;
        }

        // TODO DEBUG. Is very slowly!
        canvas.drawBitmap(rgbFrameBitmap, frameToCanvasMatrix, null);

        final RectF trackedPos = new RectF(detectionScreenRect);
        trackedPos.top -= 10;
        trackedPos.left -= 10;
        trackedPos.bottom += 10;
        trackedPos.right += 10;

        // TODO DEBUG. Some cases needs transformation
        // getFrameToCanvasMatrix().mapRect(trackedPos);

        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4);

        float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
        canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

        final String labelString =
                !TextUtils.isEmpty(label)
                        ? String.format("%s %.2f", label, (confidence))
                        : String.format("%.2f", (confidence));

        boxPaint.setTextSize(m_textSize);
        boxPaint.setStyle(Paint.Style.FILL);
        boxPaint.setTypeface(Typeface.DEFAULT_BOLD);
        boxPaint.setColor(Color.YELLOW);
        canvas.drawText(labelString + "%", trackedPos.left, trackedPos.top - 10, boxPaint);


    }


    public void trackResult(AlprDetection detection, Bitmap rgbFrameBitmap, RectF location, RectF shape, long currTimestamp) {
        logger.i("Processing result from %d", currTimestamp);
        this.detection = detection;
        this.rgbFrameBitmap=rgbFrameBitmap;
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        this.shapeScreenRect = null;
        if (shape!=null ){

            this.shapeScreenRect = shape;
        }

        if (location == null) {
            this.detectionScreenRect = null;
            this.label = "";
            this.confidence = 0;
            return;
        }

        final RectF detectionFrameRect = new RectF(location);

        final RectF detectionScreenRect = new RectF();
        rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

        logger.v(
                "Result! Frame: " + location + " mapped to screen:" + detectionScreenRect);

        this.detectionScreenRect = detectionScreenRect;
        this.label = detection.getmPlate();
        this.confidence = detection.getmConfidence();


    }

    public void draw(Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
    }
}
