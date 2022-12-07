/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
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
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.SystemClock;

import es.suma.matmatcher.util.Logger;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class AlprDetection {

    private static final int MIN_HEIGHT = 25;
    private final String mId;
    private final String mDatetime;
    private final String mFilename;
    private final Bitmap mImage;
    private final String mPlate;
    private final double mConfidence;
    private final String mRegion;
    private final String mCountry;
    private final double mRegionConfidence;
    private final int mX0;
    private final int mY0;
    private final int mX1;
    private final int mY1;
    private final double mProcessTime;
    private final RectF m_location;
    private long m_timestamp =0;
    private String m_info;

    public AlprDetection(byte[] image, String plate, double confidence, int x0, int y0, int x1, int y1, String country, String region, double regionConfidence, double processTime) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        mId = UUID.randomUUID().toString();
        mFilename=mId + ".jpg";
        mDatetime = df.format(new Date());
        mImage = bitmap;
        mPlate = plate;
        mConfidence = confidence;
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mRegion = region;
        mCountry=country;
        mRegionConfidence = regionConfidence;
        mProcessTime = processTime;
        m_location = new RectF();
        m_location.left = x0;
        m_location.top = y0;
        m_location.right = x1;
        m_location.bottom = y1;
        m_timestamp=SystemClock.uptimeMillis();
        m_info = "";
    }


    public AlprDetection(String id, String datetime, String path, String filename, String plate, double confidence, int x0, int y0, int x1, int y1, String country, String region, String info) {
        mId=id;
        mDatetime=datetime;
        mFilename=filename;
        mImage = BitmapFactory.decodeFile(path + File.separator+mFilename);
        if (mImage==null){
            Logger LOG = new Logger();
            LOG.e("Error loading image. path="+path+" filename="+filename);
        }
        mPlate = plate;
        mConfidence = confidence;
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mRegion = region;
        mCountry= country;
        mRegionConfidence = 0;
        mProcessTime = 0;
        m_location = new RectF();
        m_location.left = x0;
        m_location.top = y0;
        m_location.right = x1;
        m_location.bottom = y1;
        m_info=info;
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
        int w_height = Math.max(getmY1() - getmY0(), MIN_HEIGHT);

        if ( mImage!=null){
            return Bitmap.createBitmap(mImage, getmX0(), getmY0(), w_width, w_height);
        }
        else{
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            return Bitmap.createBitmap(100, 100, conf);
        }
    }

    public RectF getmLocation() {
        return m_location;
    }


    public String getmId() {
        return mId;
    }

    public String getmDatetime() {
        return mDatetime;
    }

    public String getmFormatDatetime() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date w_date ;
        try {
            w_date=df.parse(mDatetime);
        } catch (ParseException e) {
            e.printStackTrace();
            Calendar wCal = Calendar.getInstance();
            wCal.set(Calendar.YEAR, 2222);
            wCal.set(Calendar.MONTH, 11);
            wCal.set(Calendar.DAY_OF_MONTH, 11);
            w_date = wCal.getTime();
        }
        DateFormat df2 = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return df2.format(w_date);
    }

    public String getmFilename() {
        return mFilename;
    }

    public String getmCountry() {
        return mCountry;
    }


    public long getTimestamp() {
        return m_timestamp;
    }

    public String getInfo() {
        return m_info;
    }

    public void setInfo(String info) {
        m_info = info;
    }
}


