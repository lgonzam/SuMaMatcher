package es.suma.matmatcher;

/*
 * 2002 Suma Gestión Tributaria. Unidad Proyectos Especiales.
 *
 * This file is part of es.suma.matmarcher App
 *
 * Based on TensorFlow sample,
 * from https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection
 * sources: CameraActivity.java / DetectorActivity.java
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

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import es.suma.matmatcher.model.AlprConfig;
import es.suma.matmatcher.model.AlprDetection;
import es.suma.matmatcher.model.AlprDetector;
import es.suma.matmatcher.model.AlprPlateInfo;
import es.suma.matmatcher.model.AlprStore;
import es.suma.matmatcher.ui.BoxTracker;
import es.suma.matmatcher.ui.CameraFragment;
import es.suma.matmatcher.ui.OverlayView;
import es.suma.matmatcher.util.FileUtils;
import es.suma.matmatcher.util.Logger;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.openalpr.jni.AlprException;
import com.openalpr.jni.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity
        implements
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final int FUNCTION_MODE_MANUAL = 0;
    private static final int FUNCTION_MODE_AUTO = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private int previewWidth = 0;
    private int previewHeight = 0;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean isProcessingFrame = false;
    private int[] rgbBytes = null;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    protected ImageView bottomSheetArrowImageView;
    private Fragment fragment;
    private Context appCtx;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final boolean SAVE_PREVIEW_BITMAP = false; // VERY SLOWLY. ONLY FOR TESTING
    private OverlayView trackingOverlay;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private boolean computingDetection = false;
    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private BoxTracker tracker;
    private Button m_actionButton;
    private Button m_optButton1;
    private Button m_optButton2;
    private Button m_optButton3;
    private Button m_optButton4;
    private List<AlprDetection> m_detections;
    private AlprDetection m_bestDetection;
    private AlprConfig m_config;
    private AlprDetector m_detector;
    private Size m_previewSize;
    private Size m_cropSize;
    private int m_contIntentos = 0;
    private int m_countImg = 0;
    private String m_lastPlate;
    private long m_lastTimestamp;
    private int mLastRotation = 0;
    private int m_functionMode = 0 ;
    private boolean m_callFromApp = false ;

    private final ActivityResultLauncher<Intent> m_configResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
            });

    private int m_orientation = 0;

    private TextToSpeech m_textToSpeech;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        appCtx = getApplicationContext();
        m_config = AlprConfig.create(appCtx);
        m_previewSize = m_config.getM_previewSize();
        m_functionMode= m_config.getM_functionMode();
        m_cropSize = m_config.getM_cropSize();

        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.addBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                break;

                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    }
                });

        OrientationEventListener m_orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int orientation) {

                if (m_detector == null) return;

                if (orientation == ORIENTATION_UNKNOWN) return;

                int w_orientation = (orientation + 45) / 90 * 90;
                w_orientation = w_orientation % 360;
                int w_rotation = (90 + w_orientation) % 360;
                if (m_orientation == w_orientation) {
                    return;
                }

                m_orientation = w_orientation;
                Display w_display = getWindowManager().getDefaultDisplay();
                mLastRotation = w_display.getRotation();
                LOGGER.i("Orientation changed: " + m_orientation + " displayRotation=" + mLastRotation);

                if (w_rotation != 90) {
                    Toast.makeText(
                            CameraActivity.this,
                            getResources().getString(R.string.message_warning_onlyvertical),
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };

        if (m_orientationEventListener.canDetectOrientation()) {
            LOGGER.i("Orientation can detected");
            m_orientationEventListener.enable();
        }

        Bundle extras = getIntent().getExtras();
        m_callFromApp=false;
        if (extras != null) {
            m_callFromApp = true;
            m_functionMode = extras.getInt("functionMode");
            if (m_functionMode != FUNCTION_MODE_MANUAL && m_functionMode != FUNCTION_MODE_AUTO ){
                m_functionMode= FUNCTION_MODE_MANUAL;
                Toast.makeText(
                                CameraActivity.this,
                                getResources().getString(R.string.message_error_nomode_oncall),
                                Toast.LENGTH_LONG)
                        .show();
            }
        }

        m_textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                m_textToSpeech.setLanguage(new Locale("es", "ES"));
                m_textToSpeech.setPitch(0.5f);
            }
        });

        initUI();

    }


    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;

        imageConverter =
                () -> FileUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

        postInferenceCallback =
                () -> {
                    camera.addCallbackBuffer(bytes);
                    isProcessingFrame = false;
                };
        processImage();
    }


    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                                CameraActivity.this,
                        getResources().getString(R.string.message_camera_permission),
                                Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }


    protected void setFragment() {
            fragment =
                    new CameraFragment(this, getLayoutId(), getDesiredPreviewFrameSize());

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onClick(View v) {
    }


    protected void stopCamera() {
            CameraFragment ccF = (CameraFragment) fragment;
            ccF.stopCamera();
    }

    protected void startCamera() {
            CameraFragment ccF = (CameraFragment) fragment;
            ccF.startCamera();

    }

    protected void initUI() {
        m_actionButton = findViewById(R.id.actionButton);
        m_optButton1 = findViewById(R.id.optButton1);
        m_optButton2 = findViewById(R.id.optButton2);
        m_optButton3 = findViewById(R.id.optButton3);
        m_optButton4 = findViewById(R.id.optButton4);

        m_optButton1.setText("    -    ");
        m_optButton1.setEnabled(false);
        m_optButton1.setVisibility(View.INVISIBLE);
        m_optButton2.setText("    -    ");
        m_optButton2.setEnabled(false);
        m_optButton2.setVisibility(View.INVISIBLE);
        m_optButton3.setText("    -    ");
        m_optButton3.setEnabled(false);
        m_optButton3.setVisibility(View.INVISIBLE);
        m_optButton4.setText("    -    ");
        m_optButton4.setEnabled(false);
        m_optButton4.setVisibility(View.INVISIBLE);
        m_actionButton.setText(R.string.button_label_stop);

        m_actionButton.setOnClickListener(view -> {
            Button theButton = (Button) view;
            String text = theButton.getText().toString();
            if (text.equalsIgnoreCase(getString(R.string.button_label_continue))
                    || text.equalsIgnoreCase(getString(R.string.button_label_init))) {

                m_optButton1.setText("    -    ");
                m_optButton1.setEnabled(false);
                m_optButton1.setVisibility(View.INVISIBLE);
                m_optButton2.setText("    -    ");
                m_optButton2.setEnabled(false);
                m_optButton2.setVisibility(View.INVISIBLE);
                m_optButton3.setText("    -    ");
                m_optButton3.setEnabled(false);
                m_optButton3.setVisibility(View.INVISIBLE);
                m_optButton4.setText("    -    ");
                m_optButton4.setEnabled(false);
                m_optButton4.setVisibility(View.INVISIBLE);

                if (m_callFromApp) {
                    setResultAndExit();
                    return;
                }

                startCamera();
                computingDetection = false;
                m_actionButton.setText(R.string.button_label_stop);
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);

            } else {
                stopCamera();
                m_actionButton.setText(R.string.button_label_continue);
            }


        });

        m_optButton1.setOnClickListener(view -> selectPlate(0));

        m_optButton2.setOnClickListener(view -> selectPlate(1));

        m_optButton3.setOnClickListener(view -> selectPlate(2));

        m_optButton4.setOnClickListener(view -> selectPlate(3));

        gestureLayout = findViewById(R.id.gesture_layout);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);


    }

    private void selectPlate(int option) {
        if (m_detections != null && m_detections.size() > option) {
            m_bestDetection = m_detections.get(option);
        }

        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        m_optButton1.setText("    -    ");
        m_optButton1.setEnabled(false);
        m_optButton1.setVisibility(View.INVISIBLE);
        m_optButton2.setText("    -    ");
        m_optButton2.setEnabled(false);
        m_optButton2.setVisibility(View.INVISIBLE);
        m_optButton3.setText("    -    ");
        m_optButton3.setEnabled(false);
        m_optButton3.setVisibility(View.INVISIBLE);
        m_optButton4.setText("    -    ");
        m_optButton4.setEnabled(false);
        m_optButton4.setVisibility(View.INVISIBLE);

        boolean isSamePlate = (m_lastPlate != null && m_lastPlate.equalsIgnoreCase(m_bestDetection.getmPlate()));
        long skipMs = m_config.getM_timeSkipMs();
        long waitMs = m_config.getM_timeWaitMs();
        long elapsedMs = m_bestDetection.getTimestamp() - m_lastTimestamp;
        if (isSamePlate && elapsedMs < skipMs) {
            LOGGER.i("Skip " + m_lastPlate );
            if (m_callFromApp) {
                setResultAndExit();
            }
            else{
                //startCamera();
                computingDetection = false;
                m_actionButton.setText(R.string.button_label_stop);
            }
        } else {
            AlprPlateInfo w_plateInfo = AlprStore.getInstance(appCtx).queryPlateinfo(m_bestDetection.getmPlate());
            if (w_plateInfo!=null){
                m_bestDetection.setInfo(w_plateInfo.getmInfo());
                notifyPlate(m_bestDetection.getmPlate(), w_plateInfo.getmInfo());
            }
            else {
                notifyPlate(m_bestDetection.getmPlate(), "");
            }

            AlprStore.getInstance(appCtx).insertDetection(m_bestDetection);
            m_lastPlate = m_bestDetection.getmPlate();
            m_lastTimestamp = m_bestDetection.getTimestamp();

            if (m_callFromApp) {
                setResultAndExit();
            }
            else {

                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    //startCamera();
                    computingDetection = false;
                    m_actionButton.setText(R.string.button_label_stop);
                }, waitMs);
            }
        }
    }


    public void onPreviewSizeChosen(final Size size, final int rotation) {
        initDetector(size, rotation);
    }


    private void initDetector(final Size size, final int rotation) {
        String androidDataDir = this.getApplicationInfo().dataDir;
        Utils.copyAssetFolder(this.getAssets(), "runtime_data", androidDataDir + File.separatorChar + "runtime_data");
        Utils.copyAssetFolder(this.getAssets(), "config", androidDataDir + File.separatorChar + "config");
        //UNCOMENT ONLY FOR TESTING
        //String androidDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        //Utils.copyAssetFolder(this.getAssets(), "samples", androidDownloadDir);
        //Utils.copyAssetFolder(this.getAssets(), "samples", "/storage/emulated/0/download");

        tracker = new BoxTracker();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        int sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(m_cropSize.getWidth(), m_cropSize.getHeight(), Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                FileUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        m_cropSize.getWidth(), m_cropSize.getHeight(),
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));
        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);

        String ANDROID_DATA_DIR = getApplicationInfo().dataDir;

        m_detector = AlprDetector.getInstance(ANDROID_DATA_DIR);

        Toast.makeText(appCtx, getString(R.string.chosen_size) + size.getWidth() + "x" + size.getHeight(),
                Toast.LENGTH_LONG).show();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.opcion_config:
                showConfig();
                return true;
            case R.id.opcion_viewhistory:
                showHistory();
                return true;
            case R.id.opcion_viewplatesinfo:
                showPlatesInfo();
                return true;
            case R.id.opcion_ayuda:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("test", false);
        startActivity(intent);
    }


    private void showConfig() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("test", false);
        onStop();
        m_configResultLauncher.launch(intent);
    }


    private void showPlatesInfo() {
        Intent intent = new Intent(this, PlatesInfoActivity.class);
        intent.putExtra("test", false);
        startActivity(intent);
    }

    private void showHelp() {
        String w_helpUrl=getResources().getString(R.string.help_url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(w_helpUrl));
        startActivity(browserIntent);
    }


    protected void processImage() {
        ++timestamp;

        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }

        computingDetection = true;

        final long currTimestamp = timestamp;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        RectF cropShape = new RectF(0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
        cropToFrameTransform.mapRect(cropShape);
        tracker.trackResult(null, rgbFrameBitmap, null, cropShape, currTimestamp);

        if (SAVE_PREVIEW_BITMAP) {
            m_countImg++;
            String w_filename = String.format("img_%08d.jpg", m_countImg);
            String androidDataDir = this.getApplicationInfo().dataDir + File.separator + "images";
            FileUtils.saveBitmap(rgbFrameBitmap, androidDataDir, w_filename);
        }

        runInBackground(
                () -> {
                    LOGGER.i("INI Running detection on image " + currTimestamp);
                    final long startTime = SystemClock.uptimeMillis();

                    m_bestDetection = null;

                    m_detections = new ArrayList<>();

                    try {
                        String alprCountry = m_config.getM_alprCountry();
                        String alprRegion = m_config.getM_alprRegion();
                        int alprTopResults = m_config.getM_alprTopResults();
                        float alprMinConfidence = m_config.getM_alprMinConfidence();
                        m_detections = m_detector.recognize(alprCountry, alprRegion, alprTopResults, alprMinConfidence, croppedBitmap);
                        m_contIntentos++;
                    } catch (AlprException e) {
                        e.printStackTrace();
                        Log.e("Detector", e.getMessage());
                        Toast.makeText(appCtx, e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        stopCamera();
                        return;
                    }

                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                    if (m_detections != null && m_detections.size() > 0) {
                        // TODO. DEBUG FAIL. "Camera is being used after Camera.release() was called"
                        //stopCamera();
                        m_bestDetection = m_detections.get(0);
                    }
                    else{
                        computingDetection = false;
                        return;
                    }

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    final Canvas canvas1 = new Canvas(cropCopyBitmap);
                    final Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.0f);

                    if (m_bestDetection != null) {
                        final RectF location = m_bestDetection.getmLocation();
                        canvas1.drawRect(location, paint);
                        cropToFrameTransform.mapRect(location);
                        tracker.trackResult(m_bestDetection, rgbFrameBitmap, location, cropShape, currTimestamp);
                    }
                    trackingOverlay.postInvalidate();

                    List<AlprDetection> finalW_detections = m_detections;
                    AlprDetection finalW_bestDetection = m_bestDetection;

                        LOGGER.i("END Running detection on image " + currTimestamp);
                        runOnUiThread(() -> {
                            if ( m_functionMode == FUNCTION_MODE_MANUAL ){
                                updateUI(currTimestamp, lastProcessingTimeMs, finalW_detections, finalW_bestDetection);
                            }
                            else {
                                selectPlate (0);
                            }
                        });
                });
    }


    private void updateUI(long currTimestamp, long lastProcessingTimeMs, List<AlprDetection> detections, AlprDetection bestDetection) {
        m_optButton1.setText("    -    ");
        m_optButton1.setEnabled(false);
        m_optButton1.setVisibility(View.INVISIBLE);
        m_optButton2.setText("    -    ");
        m_optButton2.setEnabled(false);
        m_optButton2.setVisibility(View.INVISIBLE);
        m_optButton3.setText("    -    ");
        m_optButton3.setEnabled(false);
        m_optButton3.setVisibility(View.INVISIBLE);
        m_optButton4.setText("    -    ");
        m_optButton4.setEnabled(false);
        m_optButton4.setVisibility(View.INVISIBLE);

        if (detections.size() > 0) {
            String w_txtFormat = getResources().getString(R.string.message_plate_detected);

            String w_text = String.format(w_txtFormat, currTimestamp,
                    bestDetection.getmPlate(),
                    ((float) lastProcessingTimeMs / 1000.0f),
                    m_contIntentos);
            Log.i("Detector", w_text);

            m_contIntentos = 0;

            m_actionButton.setText(R.string.button_label_continue);

            if (detections.size() >= 4) {
                AlprDetection detection = detections.get(3);
                String w_titulo = String.format("%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton4.setText(w_titulo);
                m_optButton4.setEnabled(true);
                m_optButton4.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 3) {
                AlprDetection detection = detections.get(2);
                String w_titulo = String.format("%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton3.setText(w_titulo);
                m_optButton3.setEnabled(true);
                m_optButton3.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 2) {
                AlprDetection detection = detections.get(1);
                String w_titulo = String.format("%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton2.setText(w_titulo);
                m_optButton2.setEnabled(true);
                m_optButton2.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 1) {
                AlprDetection detection = detections.get(0);
                String w_titulo = String.format("%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton1.setText(w_titulo);
                m_optButton1.setEnabled(true);
                m_optButton1.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }

        } else {
            String w_txtFormat = getResources().getString(R.string.message_plate_nodetected);
            String w_text = String.format(w_txtFormat, currTimestamp,
                    ((float) lastProcessingTimeMs / 1000.0f),
                    m_contIntentos);
            LOGGER.i(w_text);
            Toast.makeText(appCtx, w_text,
                    Toast.LENGTH_SHORT).show();
        }
    }


    protected void notifyPlate (String plate, String info){
        boolean w_textToSpeech=m_config.getM_textToSpeech();
        if ( w_textToSpeech ){
            String w_toSpeak=getResources().getString(R.string.lblPlate);
            for (int i = 0; i < plate.length(); i++) {
                w_toSpeak += plate.charAt(i) + " ";
            }

            if (info!=null && !info.equalsIgnoreCase(""))
                w_toSpeak += info;
            else
                w_toSpeak+=".";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_textToSpeech.speak(w_toSpeak,TextToSpeech.QUEUE_FLUSH,null,null);
            } else {
                m_textToSpeech.speak(w_toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
            }

        }
        else{
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            if (info!=null && !info.equalsIgnoreCase("")){
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,2000);

            }
            else
            {
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP,200);

            }

        }
        String w_textPlate = getResources().getString(R.string.lblPlate) + " " + plate;
        if (info!=null && !info.equalsIgnoreCase(""))
            w_textPlate+= ": " + info;
        Toast.makeText(appCtx, w_textPlate,
                Toast.LENGTH_SHORT).show();

    }


    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }


    protected Size getDesiredPreviewFrameSize() {
        return m_previewSize;
    }

    protected void setResultAndExit(){
        Intent intent = new Intent();
        intent.putExtra("detected", (m_bestDetection!=null));
        if (m_bestDetection==null ){
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        else{
            setResult(RESULT_OK, intent);
            intent.putExtra("plate", m_bestDetection.getmPlate());
            intent.putExtra("datetime", m_bestDetection.getmDatetime());
            intent.putExtra("confidence", m_bestDetection.getmConfidence());
            intent.putExtra("info", m_bestDetection.getInfo());
            finish();
        }
    }
}
