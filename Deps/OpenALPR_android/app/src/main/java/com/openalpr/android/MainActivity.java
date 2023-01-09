package com.openalpr.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.openalpr.jni.AlprException;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static androidx.core.content.FileProvider.getUriForFile;


public class MainActivity extends AppCompatActivity {

    private final int STORAGE = 1;
    private Context appCtx;
    private ImageView imageView;
    private TableLayout resultTable;
    private final long startTime =0;
    private final long[] endTime = new long[1];
    private Bitmap originalBitmap;
    private ProgressDialog progress;
    private Uri uri;
    private boolean detection=false;
    private int rotation=0;

    private final ActivityResultLauncher<Uri> mGetContent = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result ) {
                        rotation=90;
                        makeDetection(uri);
                    }

                    // do what you need with the uri here ...
                }
            });
    private final ActivityResultLauncher<String[]> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null) {
                        uri = result;
                        rotation=0;
                        makeDetection(result);
                    }

                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        appCtx = this;

        resultTable = findViewById(R.id.resultTable);

        imageView = findViewById(R.id.imageView);

        //Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(myToolbar);

        findViewById(R.id.btnTakePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File imagePath = new File(appCtx.getFilesDir(), "/");
                File newFile = new File(imagePath, "default_image.jpg");
                uri = getUriForFile(appCtx, getApplicationContext().getPackageName() + ".provider", newFile);
                mGetContent.launch(uri);
            }
        });

        findViewById(R.id.btnLoadPicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTakePicture.launch(new String[]{"image/jpeg", "image/jpg", "image/png"});
            }
        });

    }



    private static Bitmap rotateBitmap (Bitmap source, int rotation){

        if ( rotation==0) return source;

        Matrix matrix = new Matrix();

        matrix.postRotate(90);

        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        if (result != source) {
            // Same bitmap is returned if sizes are the same
            source.recycle();
        }
        return result;
    }

    private static Bitmap resizeBitmap(Bitmap source) {
        int targetWidth = source.getWidth();
        if (targetWidth > 800) {
            targetWidth = 800;
        }
        double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
        int targetHeight = (int) (targetWidth * aspectRatio);

        Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
        if (result != source) {
            // Same bitmap is returned if sizes are the same
            source.recycle();
        }
        return result;
    }


    private static Bitmap bytesToBitmap(byte[] b) {
        return (b == null || b.length == 0) ? null : BitmapFactory
                .decodeByteArray(b, 0, b.length);
    }


    private static byte[] readUriToBytes(Context appCtx , Uri uri) throws IOException {

        InputStream iStream = appCtx.getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len ;
        while ((len = iStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        byte[] bytes = byteBuffer.toByteArray();
        return bytes;
    }


    private static String bimapToBase64(Bitmap src) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        return base64;
    }

//    public static int getCameraPhotoOrientation(Context appCtx, Uri urlImage) {
//        int rotate = 0;
//
//        try {
//            InputStream inputStream = appCtx.getContentResolver().openInputStream(urlImage);
//            ExifInterface exif = new ExifInterface(inputStream);
//
//            String exifOrientation = exif
//                    .getAttribute(ExifInterface.TAG_ORIENTATION);
//            Log.d("exifOrientation", exifOrientation);
//            int orientation = exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL);
//            Log.d(MainActivity.class.getSimpleName(), "orientation :" + orientation);
//            switch (orientation) {
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    rotate = 270;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    rotate = 180;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotate = 90;
//                    break;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return rotate;
//    }

    protected void makeDetection(Uri imageUrl) {
        detection=false;
        progress = ProgressDialog.show(this, "Loading", "Parsing result...", true);
        Picasso.get().invalidate(imageUrl);
        Picasso.get().load(imageUrl)
                .resize(800,0).rotate(rotation)
                .centerCrop().into(imageView);

        AsyncTask.execute(new Runnable() {


            @Override
            public void run() {
                byte[] bytes = new byte[0];
                try {
                    bytes = readUriToBytes(appCtx, imageUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error al cargar imagen "+imageUrl, Toast.LENGTH_LONG).show();
                    return;
                }
                Bitmap bitmap = bytesToBitmap(bytes);
                Bitmap resized = resizeBitmap(bitmap);
                Bitmap rotated = rotateBitmap (resized, rotation);
                originalBitmap = rotated;

                AlprDetector detector = AlprDetector.getInstance(appCtx.getDataDir().toString());
                List<AlprDetection> detections = null;
                try {
                    detections = detector.recognize("eu", "es", 5, rotated);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error. "+e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                } catch (AlprException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error. "+e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                UpdateUI(detections);
            }
        });
    }



    protected void UpdateUI(List<AlprDetection>  results ) {
        final int[] x1 = {0};
        final int[] x2 = {0};
        final int[] y1 = {0};
        final int[] y2 = {0};
        final String[] plate = {""};

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                        int count = resultTable.getChildCount();
                    for (int i = 1; i < count; i++) {
                        View child = resultTable.getChildAt(i);
                        if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
                    }

                    resultTable.setVisibility(View.VISIBLE);
                    if (results == null || results.size() == 0 ) {
                        resultTable.setVisibility(View.INVISIBLE);
                        Toast.makeText(MainActivity.this, "It was not possible to detect the licence plate.", Toast.LENGTH_LONG).show();
                    } else {
                        endTime[0] = System.currentTimeMillis();
                        TableLayout.LayoutParams rowLayoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
                        TableRow.LayoutParams cellLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

                        for (int i = 0; i < results.size(); ++i) {
                            AlprDetection result = results.get(i);

                            if (i == 0) { // save rectangle coordinates and plate of best result
                                x1[0] = result.getmX0();
                                y1[0] = result.getmY0();
                                x2[0] = result.getmX1();
                                y2[0] = result.getmY1();
                                plate[0] = result.getmPlate();
                                detection=true;
                            }

                            TableRow tableRow = new TableRow(appCtx);
                            tableRow.setLayoutParams(rowLayoutParams);

                            if (result.getmConfidence() < 60)
                                tableRow.setBackgroundColor(Color.RED);
                            else if (result.getmConfidence() < 85)
                                tableRow.setBackgroundColor(Color.YELLOW);
                            else if (result.getmConfidence() >= 85)
                                tableRow.setBackgroundColor(Color.GREEN);

                            TextView cellValue = new TextView(appCtx);
                            cellValue.setTypeface(null, Typeface.BOLD);
                            cellValue.setText(result.getmPlate());
                            cellValue.setLayoutParams(cellLayoutParams);
                            tableRow.addView(cellValue);

                            cellValue = new TextView(appCtx);
                            cellValue.setTypeface(null, Typeface.BOLD);
                            cellValue.setText(String.format("%.2f", result.getmConfidence()) + "%");
                            cellValue.setLayoutParams(cellLayoutParams);
                            tableRow.addView(cellValue);

                            String region = "eu_es";
                            cellValue = new TextView(appCtx);
                            cellValue.setTypeface(null, Typeface.BOLD);
                            cellValue.setText(region.length() == 1 ? "n/a" : region);
                            cellValue.setLayoutParams(cellLayoutParams);
                            tableRow.addView(cellValue);

                            cellValue = new TextView(appCtx);
                            cellValue.setTypeface(null, Typeface.BOLD);
                            cellValue.setText(String.format("%.2f", ((result.getmProcessTime() / 1000.0) % 60)) + " s");
                            cellValue.setLayoutParams(cellLayoutParams);
                            tableRow.addView(cellValue);

                            resultTable.addView(tableRow);
                        }
                        resultTable.invalidate();
                        Toast.makeText(appCtx, "Processing time: " + String.format("%.2f", (((endTime[0] - startTime) / 1000.0) % 60)) + " s", Toast.LENGTH_LONG).show();
                    }
                }
            });


        progress.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Picasso requires permission.WRITE_EXTERNAL_STORAGE
                if (imageView.getDrawable() != null && detection) {
                    Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                    float viewWidth = bitmap.getWidth();
                    float viewHeigth = bitmap.getHeight();
                    float originalWidth = originalBitmap.getWidth();
                    float originalHeigth = originalBitmap.getHeight();

                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(Color.GREEN);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);

                    // map rectangle coordinates to imageview
                    int p1_x = (int) ((x1[0] * viewWidth) / originalWidth) - 20;
                    int p1_y = (int) ((y1[0] * viewHeigth) / originalHeigth) - 20;
                    int p2_x = (int) ((x2[0] * viewWidth) / originalWidth) + 20;
                    int p2_y = (int) ((y2[0] * viewHeigth) / originalHeigth) + 20;
                    canvas.drawRect(new Rect(p1_x, p1_y, p2_x, p2_y), paint);

                    paint.setTextSize(70);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setColor(Color.YELLOW);
                    canvas.drawText(plate[0], p1_x, p1_y - 10, paint);
                    imageView.setImageBitmap(bitmap);
                }
            }
        });
    }


    private void checkPermission() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, "Storage access needed to manage the picture.", Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            ActivityCompat.requestPermissions(this, params, STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORAGE: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for WRITE_EXTERNAL_STORAGE
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (storage) {
                    // permission was granted, yay!
                } else {
                    // Permission Denied
                    Toast.makeText(this, "Storage permission is needed to analyse the picture.", Toast.LENGTH_LONG).show();
                }
            }
            default:
                break;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (uri != null) {// Picasso does not seem to have an issue with a null value, but to be safe
            Picasso.get().load(uri)
                    .resize(800,0).rotate(rotation)
                    .centerCrop().into(imageView);
        }
    }

}
