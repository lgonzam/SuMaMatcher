/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
 *
 * This file is part of RecMatApp.
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import es.suma.matmatcher.util.FileUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AlprStore {

    private AlprDbHelper dbHelper ;
    private static AlprStore theInstance = null;
    private final String mImagesDir;

    private AlprStore(Context appCxt) {
        mImagesDir=appCxt.getApplicationInfo().dataDir + File.separator + "images";
        dbHelper = new AlprDbHelper(appCxt);
    }


    private static class DetectionEntry implements BaseColumns {
        public static final String TABLE_NAME = "alpr_detections";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_DATETIME = "datetime";
        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_PLATE = "plate";
        public static final String COLUMN_CONFIDENCE = "confidence";
        public static final String COLUMN_LOCATION_LEFT = "location_left";
        public static final String COLUMN_LOCATION_TOP = "location_top";
        public static final String COLUMN_LOCATION_RIGHT = "location_right";
        public static final String COLUMN_LOCATION_BOTTOM = "location_bottom";
        public static final String COLUMN_COUNTRY = "country";
        public static final String COLUMN_REGION = "region";
        public static final String COLUMN_INFO = "info";
    }

    private static class PlatesInfoEntry implements BaseColumns {
        public static final String TABLE_NAME = "alpr_plates_info";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_DATETIME = "datetime";
        public static final String COLUMN_PLATE = "plate";
        public static final String COLUMN_INFO = "info";
        public static final String COLUMN_EXPIRES = "expires";
    }

    private static class AlprDbHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "Plates.db";
        private static final String SQL_DETECTION_ENTRY =
                "CREATE TABLE " + DetectionEntry.TABLE_NAME + " (" +
                        DetectionEntry.COLUMN_ID + " TEXT PRIMARY KEY," +
                        DetectionEntry.COLUMN_DATETIME + " TEXT," +
                        DetectionEntry.COLUMN_FILENAME + " TEXT," +
                        DetectionEntry.COLUMN_PLATE + " TEXT," +
                        DetectionEntry.COLUMN_CONFIDENCE + " REAL," +
                        DetectionEntry.COLUMN_LOCATION_LEFT + " REAL," +
                        DetectionEntry.COLUMN_LOCATION_TOP + " REAL," +
                        DetectionEntry.COLUMN_LOCATION_RIGHT + " REAL," +
                        DetectionEntry.COLUMN_LOCATION_BOTTOM + " REAL," +
                        DetectionEntry.COLUMN_COUNTRY + " TEXT," +
                        DetectionEntry.COLUMN_REGION + " TEXT," +
                        DetectionEntry.COLUMN_INFO + " TEXT)";

        private static final String SQL_PLATE_INFO_ENTRY =
                "CREATE TABLE " + PlatesInfoEntry.TABLE_NAME + " (" +
                        PlatesInfoEntry.COLUMN_ID + " TEXT PRIMARY KEY," +
                        PlatesInfoEntry.COLUMN_DATETIME + " TEXT," +
                        PlatesInfoEntry.COLUMN_PLATE + " TEXT," +
                        PlatesInfoEntry.COLUMN_INFO + " TEXT," +
                        PlatesInfoEntry.COLUMN_EXPIRES + " TEXT)";

        private static final String SQL_DELETE_PLATES_ENTRIES =
                "DROP TABLE IF EXISTS " + DetectionEntry.TABLE_NAME;

        private static final String SQL_DELETE_INFO_ENTRIES =
                "DROP TABLE IF EXISTS " + PlatesInfoEntry.TABLE_NAME;

        public AlprDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_DETECTION_ENTRY);
            db.execSQL(SQL_PLATE_INFO_ENTRY);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion==1 && newVersion==2){
                db.execSQL("ALTER TABLE "+DetectionEntry.TABLE_NAME+" ADD "+DetectionEntry.COLUMN_INFO);
            }
            else {
                db.execSQL(SQL_DELETE_PLATES_ENTRIES);
                db.execSQL(SQL_DELETE_INFO_ENTRIES);
                onCreate(db);
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }


    public long insertDetection(AlprDetection record) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();


        ContentValues values = new ContentValues();

        values.put(DetectionEntry.COLUMN_ID, record.getmId());
        values.put(DetectionEntry.COLUMN_DATETIME, record.getmDatetime());
        values.put(DetectionEntry.COLUMN_FILENAME, record.getmFilename());
        values.put(DetectionEntry.COLUMN_PLATE, record.getmPlate());
        values.put(DetectionEntry.COLUMN_CONFIDENCE, record.getmConfidence());
        values.put(DetectionEntry.COLUMN_LOCATION_LEFT, record.getmX0());
        values.put(DetectionEntry.COLUMN_LOCATION_TOP, record.getmY0());
        values.put(DetectionEntry.COLUMN_LOCATION_RIGHT, record.getmX1());
        values.put(DetectionEntry.COLUMN_LOCATION_BOTTOM, record.getmY1());
        values.put(DetectionEntry.COLUMN_COUNTRY, record.getmCountry());
        values.put(DetectionEntry.COLUMN_REGION, record.getmRegion());
        values.put(DetectionEntry.COLUMN_INFO, record.getInfo());
        long newRowId = db.insert(DetectionEntry.TABLE_NAME, null, values);

        FileUtils.saveBitmap(record.getmImage(), mImagesDir, record.getmFilename());

        return newRowId;
    }


    public int deleteAllDetections() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DetectionEntry.COLUMN_PLATE + " IS NOT NULL";
        String[] selectionArgs = {};
        return db.delete(DetectionEntry.TABLE_NAME, selection, selectionArgs);
    }


    public List<AlprDetection> queryDetection(Date date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DetectionEntry.COLUMN_ID,
                DetectionEntry.COLUMN_DATETIME,
                DetectionEntry.COLUMN_FILENAME,
                DetectionEntry.COLUMN_PLATE,
                DetectionEntry.COLUMN_CONFIDENCE,
                DetectionEntry.COLUMN_LOCATION_LEFT,
                DetectionEntry.COLUMN_LOCATION_TOP,
                DetectionEntry.COLUMN_LOCATION_RIGHT,
                DetectionEntry.COLUMN_LOCATION_BOTTOM,
                DetectionEntry.COLUMN_COUNTRY,
                DetectionEntry.COLUMN_REGION,
                DetectionEntry.COLUMN_INFO
        };

        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'");
        String w_fecha = sdf.format(date);
        String selection = DetectionEntry.COLUMN_DATETIME + " >= ?";
        String[] selectionArgs = {w_fecha};

        String sortOrder =
                DetectionEntry.COLUMN_DATETIME + " DESC";

        Cursor cursor = db.query(
                DetectionEntry.TABLE_NAME,   // The table to queryDetection
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        List<AlprDetection> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            String w_id = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_ID));
            String w_datetime = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_DATETIME));
            String w_filename = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_FILENAME));
            String w_plate = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_PLATE));
            double w_confidence = cursor.getDouble(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_CONFIDENCE));
            int w_location_left = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_LEFT));
            int w_location_top = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_TOP));
            int w_location_right = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_RIGHT));
            int w_location_bottom = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_BOTTOM));
            String w_country = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_COUNTRY));
            String w_region = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_REGION));
            String w_info= cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_INFO));

            AlprDetection detection = new AlprDetection(w_id, w_datetime, mImagesDir, w_filename, w_plate,
                    w_confidence, w_location_left, w_location_top, w_location_right, w_location_bottom,
                    w_country, w_region, w_info);
            items.add(detection);
        }
        cursor.close();
        return items;
    }

    public AlprDetection queryDetection(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DetectionEntry.COLUMN_ID,
                DetectionEntry.COLUMN_DATETIME,
                DetectionEntry.COLUMN_FILENAME,
                DetectionEntry.COLUMN_PLATE,
                DetectionEntry.COLUMN_CONFIDENCE,
                DetectionEntry.COLUMN_LOCATION_LEFT,
                DetectionEntry.COLUMN_LOCATION_TOP,
                DetectionEntry.COLUMN_LOCATION_RIGHT,
                DetectionEntry.COLUMN_LOCATION_BOTTOM,
                DetectionEntry.COLUMN_COUNTRY,
                DetectionEntry.COLUMN_REGION,
                DetectionEntry.COLUMN_INFO
        };

        String selection = DetectionEntry.COLUMN_ID + " = ?";
        String[] selectionArgs = { id };

        String sortOrder =
                DetectionEntry.COLUMN_DATETIME + " DESC";

        Cursor cursor = db.query(
                DetectionEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        AlprDetection detection=null;
        while (cursor.moveToNext()) {
            String w_id = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_ID));
            String w_datetime = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_DATETIME));
            String w_filename = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_FILENAME));
            String w_plate = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_PLATE));
            double w_confidence = cursor.getDouble(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_CONFIDENCE));
            int w_location_left = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_LEFT));
            int w_location_top = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_TOP));
            int w_location_right = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_RIGHT));
            int w_location_bottom = cursor.getInt(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_LOCATION_BOTTOM));
            String w_country = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_COUNTRY));
            String w_region = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_REGION));
            String w_info = cursor.getString(cursor.getColumnIndexOrThrow(DetectionEntry.COLUMN_INFO));

            detection = new AlprDetection(w_id, w_datetime, mImagesDir, w_filename, w_plate,
                    w_confidence, w_location_left, w_location_top, w_location_right, w_location_bottom,
                    w_country, w_region, w_info);
        }
        cursor.close();
        return detection;
    }



    public long insertPlateInfo(AlprPlateInfo record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(PlatesInfoEntry.COLUMN_ID, record.getmId());
        values.put(PlatesInfoEntry.COLUMN_DATETIME, record.getmDatetime());
        values.put(PlatesInfoEntry.COLUMN_PLATE, record.getmPlate());
        values.put(PlatesInfoEntry.COLUMN_INFO, record.getmInfo());
        values.put(PlatesInfoEntry.COLUMN_EXPIRES, record.getmExpires());

        long newRowId = db.insert(PlatesInfoEntry.TABLE_NAME, null, values);
        return newRowId;
    }



    public int deleteAllPlateInfo() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = PlatesInfoEntry.COLUMN_PLATE + " IS NOT NULL";
        String[] selectionArgs = {};
        return db.delete(PlatesInfoEntry.TABLE_NAME, selection, selectionArgs);
    }


    public List<AlprPlateInfo> queryPlateInfo(Date date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                PlatesInfoEntry.COLUMN_ID,
                PlatesInfoEntry.COLUMN_DATETIME,
                PlatesInfoEntry.COLUMN_PLATE,
                PlatesInfoEntry.COLUMN_INFO,
                PlatesInfoEntry.COLUMN_EXPIRES

        };

        String selection = "";
        String[] selectionArgs = {  };

        String sortOrder =
                DetectionEntry.COLUMN_DATETIME + " DESC";

        Cursor cursor = db.query(
                PlatesInfoEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        List<AlprPlateInfo> items = new ArrayList<>();
        while (cursor.moveToNext()) {

            String w_id = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_ID));
            String w_datetime = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_DATETIME));
            String w_plate = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_PLATE));
            String w_info = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_INFO));
            String w_expires = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_EXPIRES));

            AlprPlateInfo info = new AlprPlateInfo(w_id, w_datetime, w_plate, w_info, w_expires);
            items.add(info);
        }
        cursor.close();
        return items;
    }

    public AlprPlateInfo queryPlateinfo(String plate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                PlatesInfoEntry.COLUMN_ID,
                PlatesInfoEntry.COLUMN_DATETIME,
                PlatesInfoEntry.COLUMN_PLATE,
                PlatesInfoEntry.COLUMN_INFO,
                PlatesInfoEntry.COLUMN_EXPIRES

        };
        Calendar cal = Calendar.getInstance();
        Date w_date = cal.getTime();
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'");
        String w_fecha = sdf.format(w_date);
        String selection = PlatesInfoEntry.COLUMN_PLATE + " = ? AND " + PlatesInfoEntry.COLUMN_EXPIRES + ">= ?";
        String[] selectionArgs = { plate, w_fecha };

        Cursor cursor = db.query(
                PlatesInfoEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        AlprPlateInfo plateInfo=null;
        while (cursor.moveToNext()) {
            String w_id = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_ID));
            String w_datetime = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_DATETIME));
            String w_info = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_INFO));
            String w_plate = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_PLATE));
            String w_expires = cursor.getString(cursor.getColumnIndexOrThrow(PlatesInfoEntry.COLUMN_EXPIRES));

            plateInfo = new AlprPlateInfo(w_id, w_datetime, w_plate, w_info, w_expires);
        }
        cursor.close();
        return plateInfo;
    }



    private void destroy() {
        dbHelper.close();
        dbHelper = null;
    }


    public static AlprStore getInstance(Context ctx){
        if ( theInstance == null)
        {
            theInstance = new AlprStore (ctx);
        }
        return theInstance;
    }


}
