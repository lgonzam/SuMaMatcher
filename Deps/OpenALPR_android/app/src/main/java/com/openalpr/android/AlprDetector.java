package com.openalpr.android;

import android.graphics.Bitmap;

import com.openalpr.jni.Alpr;
import com.openalpr.jni.AlprCoordinate;
import com.openalpr.jni.AlprException;
import com.openalpr.jni.AlprPlate;
import com.openalpr.jni.AlprPlateResult;
import com.openalpr.jni.AlprResults;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AlprDetector {

    private final String runtimeDataDir;
    private final String dataDir;
    private static AlprDetector instance;

    private AlprDetector(String dataDir) {

        this.dataDir=dataDir;

        runtimeDataDir=dataDir + File.separatorChar + "runtime_data";
    }

    private static byte[] bitmapToBytes (Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        //bmp.recycle();
        return byteArray;
    }

    public List<AlprDetection> recognize(String country, String region, int topN, Bitmap bmp) throws IOException, AlprException {
        byte[] imageData = bitmapToBytes(bmp);
        return recognize (country, region, topN, imageData);
    }

    public List<AlprDetection> recognize(String country, String region, int topN, byte[] imageData) throws IOException, AlprException {

        String w_configFile = dataDir + File.separatorChar + "config" + File.separatorChar + "openalpr.conf";
        String w_runtimeDataDir = dataDir + File.separatorChar + "runtime_data";

        Alpr alpr = new Alpr(country, w_configFile, w_runtimeDataDir);

        alpr.setTopN(topN);
        alpr.setDefaultRegion(region);
        alpr.setDetectRegion(true);

        AlprResults results = null;
        try {
            results = alpr.recognize(imageData);
        } catch (AlprException e) {
            e.printStackTrace();
            throw e;
        }

        //("OpenALPR Version: " + alpr.getVersion());
        //System.out.println("Image Size: " + results.getImgWidth() + "x" + results.getImgHeight());
        //System.out.println("Processing Time: " + results.getTotalProcessingTimeMs() + " ms");
        //System.out.println("Found " + results.getPlates().size() + " results");
        alpr.unload();

        //System.out.format("  %-15s%-8s\n", "Plate Number", "Confidence");

        List<AlprDetection> w_detections = new ArrayList<AlprDetection>();
        for (AlprPlateResult result : results.getPlates())
        {
            for (AlprPlate plate : result.getTopNPlates()) {
                String w_plate = plate.getCharacters();
                Double w_plateConfidence=new Double (plate.getOverallConfidence());
                String w_region=result.getRegion();
                Double w_regionConfidence=new Double (result.getRegionConfidence());
                Double w_processing_time = new Double (result.getProcessingTimeMs());

                int w_x0=result.getPlatePoints().get(0).getX();
                int w_y0=result.getPlatePoints().get(0).getY();
                int w_x1=result.getPlatePoints().get(2).getX();
                int w_y1=result.getPlatePoints().get(2).getY();

                AlprDetection w_res = new AlprDetection(
                        imageData,
                        w_plate,
                        w_plateConfidence,
                        w_x0,
                        w_y0,
                        w_x1,
                        w_y1,
                        w_region,
                        w_regionConfidence,
                        w_processing_time
                );
                w_detections.add(w_res);
            }
        }
        return w_detections;
    }

    public synchronized static AlprDetector getInstance(String dataDir) {
        // Return Full path to the directory where native JNI libraries are stored.

        if (instance == null) {
            instance = new AlprDetector(dataDir);
        }


        return instance;
    }
}
