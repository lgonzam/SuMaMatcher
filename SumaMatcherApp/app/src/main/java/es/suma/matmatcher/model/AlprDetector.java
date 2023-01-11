/*
 * 2022 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
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


package es.suma.matmatcher.model;

import android.graphics.Bitmap;

import es.suma.matmatcher.util.FileUtils;
import com.openalpr.jni.Alpr;
import com.openalpr.jni.AlprException;
import com.openalpr.jni.AlprPlate;
import com.openalpr.jni.AlprPlateResult;
import com.openalpr.jni.AlprResults;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AlprDetector {

    private final String dataDir;
    private static AlprDetector instance;

    private AlprDetector(String dataDir) {

        this.dataDir=dataDir;

    }

    public List<AlprDetection> recognize(String country, String region, int topN, float minConfidence, Bitmap bmp) throws AlprException {
        byte[] imageData = FileUtils.bitmapToBytes(bmp);
        return recognize (country, region, topN, minConfidence,imageData);
    }


    public List<AlprDetection> recognize(String country, String region, int topN, float minConfidence, byte[] imageData) throws AlprException {

        String w_configFile = dataDir + File.separatorChar + "config" + File.separatorChar + "openalpr.conf";
        String w_runtimeDataDir = dataDir + File.separatorChar + "runtime_data";

        Alpr alpr = new Alpr(country, w_configFile, w_runtimeDataDir);

        alpr.setTopN(topN);
        alpr.setDefaultRegion(region);
        alpr.setDetectRegion(true);

        AlprResults results ;
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

        List<AlprDetection> w_detections = new ArrayList<>();
        for (AlprPlateResult result : results.getPlates())
        {
            for (AlprPlate plate : result.getTopNPlates()) {
                String w_plate = plate.getCharacters();
                double w_plateConfidence= plate.getOverallConfidence();

                if ( w_plateConfidence < minConfidence){
                    continue;
                }
                String w_region=result.getRegion();
                double w_regionConfidence=  result.getRegionConfidence();
                double w_processing_time = result.getProcessingTimeMs();

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
                        country,
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

        if (instance == null) {
            instance = new AlprDetector(dataDir);
        }


        return instance;
    }
}
