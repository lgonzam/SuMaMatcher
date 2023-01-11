package es.suma.matmatcher;

/*
 * 2022 Suma Gestión Tributaria. Unidad Proyectos Especiales.
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
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.openalpr.jni.AlprException;
import com.openalpr.jni.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.suma.matmatcher.model.AlprConfig;
import es.suma.matmatcher.model.AlprDetection;
import es.suma.matmatcher.model.AlprDetector;
import es.suma.matmatcher.model.AlprPlateInfo;
import es.suma.matmatcher.model.AlprStore;
import es.suma.matmatcher.ui.BoxTracker;
import es.suma.matmatcher.util.FileUtils;
import es.suma.matmatcher.util.ImageUtils;
import es.suma.matmatcher.util.Logger;

public class CameraActivity extends AppCompatActivity
        implements
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final Logger LOGGER = new Logger();
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final int FUNCTION_MODE_MANUAL = 0;
    private static final int FUNCTION_MODE_AUTO = 1;
    private static final boolean SAVE_PREVIEW_BITMAP = false; // VERY SLOWLY. ONLY FOR TESTING
    private static final int SUPPORTED_ORIENTATION = 90;

    private int m_previewWidth = 0;
    private int m_previewHeight = 0;

    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    protected ImageView bottomSheetArrowImageView;
    private Context appCtx;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private long timestamp = 0;
    private Button m_actionButton;
    private Button m_optButton1;
    private Button m_optButton2;
    private Button m_optButton3;
    private Button m_optButton4;
    private TextView m_textInfo;
    private List<AlprDetection> m_detections;
    private AlprDetection m_bestDetection;
    private AlprConfig m_config;
    private AlprDetector m_detector;
    private Size m_cropSize;
    private int m_contIntentos = 0;
    private String m_lastPlate;
    private long m_lastTimestamp;
    private int mLastRotation = 0;
    private int m_functionMode = 0;
    private boolean m_callFromApp = false;
    private ImageView imageOverlay;
    private ImageView cropImage;

    private PreviewView previewCamera;
    private ExecutorService cameraAnalyserExecutor;
    private ExecutorService cameraViewerExecutor;
    private final byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;

    private long mLastAnalysisResultTime = 0;
    Size m_targetResolution = null;


    private final ActivityResultLauncher<Intent> m_configResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
            });

    private int m_orientation = 0;
    private boolean m_rgbMode=false;
    private TextToSpeech m_textToSpeech;
    private ImageProxy m_currentImage;
    private boolean isTracking = false;
    private String[] m_supportedSizes = new String[]{"720x720"};
    private boolean cameraRunning =false;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private int m_displayRotation;
    private CameraControl m_camControl;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        appCtx = getApplicationContext();
        m_config = AlprConfig.create(appCtx);
        m_functionMode = m_config.getM_functionMode();
        m_rgbMode =  m_config.getM_colorMode().equalsIgnoreCase("rgb");
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        previewCamera = findViewById(R.id.previewView);
        imageOverlay = findViewById(R.id.imageOverlay);
        cropImage = findViewById(R.id.cropImage);

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
                            case BottomSheetBehavior.STATE_HALF_EXPANDED:

                            case BottomSheetBehavior.STATE_DRAGGING:
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
        m_callFromApp = false;
        if (extras != null) {
            m_callFromApp = true;
            m_functionMode = extras.getInt("functionMode");
            if (m_functionMode != FUNCTION_MODE_MANUAL && m_functionMode != FUNCTION_MODE_AUTO) {
                m_functionMode = FUNCTION_MODE_MANUAL;
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

        if (hasPermission()) {
            startCamera();
        } else {
            requestPermission();
        }


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


    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);


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


    @Override
    public void onRequestPermissionsResult(
            final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (!allPermissionsGranted(grantResults)) {
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

    protected Size getCropSize (Size cropSize, Size previewSize){
            if ( previewSize.getWidth()<cropSize.getWidth ()){
                return previewSize;
            }
            else{
                return cropSize;
            }
    }

    protected Size convertSize(Size size) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            return size;
        } else {
            // In portrait
            return new Size(size.getHeight(), size.getWidth());
        }
    }


    protected int getDisplayRotation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_0:
                return 0;
            default:
                throw new IllegalStateException("Unexpected value: " + getWindowManager().getDefaultDisplay().getRotation());
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onClick(View v) {
    }


    protected void stopCamera() {

        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            cameraRunning =false;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void startCamera() {
        Log.d("permission", "starting camera");
        cameraAnalyserExecutor = Executors.newSingleThreadExecutor();
        cameraViewerExecutor = Executors.newSingleThreadExecutor();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                checkPreviewSizes(processCameraProvider);
                bindingPreview(processCameraProvider);
                cameraRunning =true;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(appCtx, e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));

    }


    private void bindingPreview(ProcessCameraProvider processCameraProvider) {
        LOGGER.i("bindingPreview() Called");

        m_targetResolution = convertSize(m_config.getM_previewSize());
        m_cropSize=m_config.getM_cropSize();
        m_cropSize= convertSize(m_cropSize);
        m_cropSize= getCropSize(m_cropSize, m_targetResolution);

        m_displayRotation = getDisplayRotation();

        Preview preview =
                new Preview.Builder()
                        .setTargetResolution(m_targetResolution)
                        .setTargetRotation(m_displayRotation)
                        .build();

        preview.setSurfaceProvider(cameraViewerExecutor, previewCamera.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        int w_colorMode =  m_rgbMode ? ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                                     : ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888;
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(m_targetResolution)
                .setOutputImageFormat(w_colorMode)
                .build();

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .build();

        imageAnalysis.setAnalyzer(cameraAnalyserExecutor, image -> {
            final long currTimestamp = timestamp;

            runOnUiThread(() -> trackResult(false));

            LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread. size = %d x %d", image.getWidth(), image.getHeight());
            m_currentImage = image;
            ++timestamp;
            if (m_rgbMode) {
                IntBuffer tmp = image.getPlanes()[0].getBuffer().asIntBuffer();
                if (tmp.remaining() != rgbBytes.length) {
                    LOGGER.e("Buffer: Src " + tmp.remaining() + " Dst " + rgbBytes.length);
                    readyForNextImage();
                    return;
                } else {
                    tmp.rewind();
                    tmp.get(rgbBytes);
                }
                ImageUtils.convertImageProxyToBitmap(image, rgbFrameBitmap);
            }
            else{

                final ImageProxy.PlaneProxy[] planes = image.getPlanes();
                fillBytes(planes, yuvBytes);
                final int yRowStride = planes[0].getRowStride();
                final int uvRowStride = planes[1].getRowStride();
                final int uvPixelStride = planes[1].getPixelStride();

                ImageUtils.convertYUV420ToARGB8888(
                        yuvBytes[0],
                        yuvBytes[1],
                        yuvBytes[2],
                        m_previewWidth,
                        m_previewHeight,
                        yRowStride,
                        uvRowStride,
                        uvPixelStride,
                        rgbBytes);
                rgbFrameBitmap.setPixels(rgbBytes, 0, m_previewWidth, 0, 0, m_previewWidth, m_previewHeight);
            }

            int imageRotation = image.getImageInfo().getRotationDegrees();
            Matrix postRot = new Matrix();
            postRot.postRotate(imageRotation);
            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

            if (SAVE_PREVIEW_BITMAP || m_config.getM_debugMode()) {
                //String androidDataDir = getApplicationInfo().dataDir + File.separator + "images";
                //FileUtils.saveBitmap(rgbFrameBitmap, androidDataDir, w_filename);
                File galleryDirectory=Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                );
                //FileUtils.saveBitmap(rgbFrameBitmap, androidDataDir, w_filename);
                String w_filename = String.format(Locale.getDefault(), "img_%08d_full.jpg", currTimestamp);
                FileUtils.saveBitmap(rgbFrameBitmap, galleryDirectory.getAbsolutePath(), w_filename);
                w_filename = String.format(Locale.getDefault(), "img_%08d_crop.jpg", currTimestamp);
                FileUtils.saveBitmap(croppedBitmap, galleryDirectory.getAbsolutePath(), w_filename);
            }

            makeDetection(croppedBitmap, currTimestamp);

            List<AlprDetection> finalW_detections = m_detections;
            AlprDetection finalW_bestDetection = m_bestDetection;

            runOnUiThread(() -> {
                long duration = SystemClock.elapsedRealtime() - mLastAnalysisResultTime;
                double fps;

                if (duration > 0)
                    fps = 1000.f / duration;
                else
                    fps = 1000.f;

                m_textInfo.setText(String.format(Locale.getDefault(),
                        "%s %s(%s) %.1f fps. %d img en %.2f seg.", m_config.getM_colorMode(), m_targetResolution, m_cropSize, fps, currTimestamp, ((float) lastProcessingTimeMs / 1000.0f)));
                mLastAnalysisResultTime = SystemClock.elapsedRealtime();


                if (finalW_detections.size() > 0) {
                    if (m_functionMode == FUNCTION_MODE_MANUAL) {
                        updateUI(currTimestamp, lastProcessingTimeMs, finalW_detections, finalW_bestDetection);
                    } else {
                        selectPlate(finalW_detections, finalW_bestDetection, 0);
                    }
                } else {
                    readyForNextImage();
                }
            });

        });

        processCameraProvider.unbindAll();

        androidx.camera.core.Camera w_cam= processCameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        m_camControl = w_cam.getCameraControl();
        m_camControl.setZoomRatio(m_config.getZoom());

        // PREVIEW
        m_previewWidth = Objects.requireNonNull(imageAnalysis.getResolutionInfo()).getResolution().getWidth();
        m_previewHeight = imageAnalysis.getResolutionInfo().getResolution().getHeight();
        //m_previewWidth=m_targetResolution.getWidth();
        //m_previewHeight=m_targetResolution.getHeight();

        initDetector(m_previewWidth, m_previewHeight);

    }

    private void initDetector(int previewWidth, int previewHeight) {
        String androidDataDir = this.getApplicationInfo().dataDir;
        Utils.copyAssetFolder(this.getAssets(), "runtime_data", androidDataDir + File.separatorChar + "runtime_data");
        Utils.copyAssetFolder(this.getAssets(), "config", androidDataDir + File.separatorChar + "config");
        //UNCOMENT ONLY FOR TESTING
        //String androidDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        //Utils.copyAssetFolder(this.getAssets(), "samples", androidDownloadDir);
        //Utils.copyAssetFolder(this.getAssets(), "", "/storage/emulated/0/download");

        int sensorOrientation = CameraActivity.SUPPORTED_ORIENTATION - getDisplayRotation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        croppedBitmap = Bitmap.createBitmap(m_cropSize.getWidth(), m_cropSize.getHeight(), Bitmap.Config.ARGB_8888);

        String ANDROID_DATA_DIR = getApplicationInfo().dataDir;

        m_detector = AlprDetector.getInstance(ANDROID_DATA_DIR);

        rgbBytes = new int[previewWidth * previewHeight];

        frameToCropTransform =
                FileUtils.getTransformationMatrix(
                        m_previewWidth, m_previewHeight,
                        m_cropSize.getWidth(), m_cropSize.getHeight(),
                        sensorOrientation, true);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        setCropImage();

        Toast.makeText(appCtx, getString(R.string.chosen_size) + previewWidth + "x" + previewHeight,
                Toast.LENGTH_LONG).show();

    }


    protected void trackResult(boolean trackOrUnTrack) {

        if (!trackOrUnTrack || m_bestDetection == null) {
            if (!isTracking) return;
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bmp = Bitmap.createBitmap(m_previewWidth, m_previewHeight, conf);
            imageOverlay.setImageBitmap(bmp);
            isTracking = false;
        } else {

            isTracking = true;
            //Matrix postRot=new Matrix ();
            //postRot.postRotate(m_imageRotation);
            //Bitmap w_mutableBitmap = Bitmap.createBitmap(rgbFrameBitmap, 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), postRot, false);
            Bitmap w_mutableBitmap = Bitmap.createBitmap(croppedBitmap);
            imageOverlay.setImageBitmap(w_mutableBitmap);
            BoxTracker boxTracker = new BoxTracker(40);
            boxTracker.setFrameConfiguration(croppedBitmap.getWidth(), croppedBitmap.getHeight(), 0);
            boxTracker.trackResult(m_bestDetection, croppedBitmap, m_bestDetection.getmLocation(), null, 0);
            boxTracker.draw(w_mutableBitmap);
        }
        imageOverlay.postInvalidate();
    }


    protected void makeDetection(Bitmap image, long currTimestamp) {
        LOGGER.i("INI Running detection on image " + currTimestamp);
        final long startTime = SystemClock.uptimeMillis();
        m_bestDetection = null;
        m_detections = new ArrayList<>();

        try {
            String alprCountry = m_config.getM_alprCountry();
            String alprRegion = m_config.getM_alprRegion();
            int alprTopResults = m_config.getM_alprTopResults();
            float alprMinConfidence = m_config.getM_alprMinConfidence();
            m_detections = m_detector.recognize(alprCountry, alprRegion, alprTopResults, alprMinConfidence, image);
            m_contIntentos++;
        } catch (AlprException e) {
            e.printStackTrace();
            Log.e("Detector", e.getMessage());
            Toast.makeText(appCtx, e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        if (m_detections != null && m_detections.size() > 0) {
            m_bestDetection = m_detections.get(0);
        }

        LOGGER.i("END Running detection on image " + currTimestamp);
    }


    protected void initUI() {
        m_actionButton = findViewById(R.id.actionButton);
        m_optButton1 = findViewById(R.id.optButton1);
        m_optButton2 = findViewById(R.id.optButton2);
        m_optButton3 = findViewById(R.id.optButton3);
        m_optButton4 = findViewById(R.id.optButton4);
        m_textInfo = findViewById(R.id.text_info);

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

        m_actionButton.setOnClickListener(this::actionButton);

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


    private void actionButton(View view) {
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

            if (!cameraRunning)
                startCamera();
            else
                readyForNextImage();

            m_actionButton.setText(R.string.button_label_stop);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);

        } else {
            stopCamera();
            m_actionButton.setText(R.string.button_label_continue);
        }


    }

    private void readyForNextImage() {
        if (m_currentImage != null)
            m_currentImage.close();

    }

    private void selectPlate (int option){
        List<AlprDetection> finalW_detections = m_detections;
        AlprDetection finalW_bestDetection = m_bestDetection;
        selectPlate (finalW_detections, finalW_bestDetection, option);
    }

    private void selectPlate(List<AlprDetection> detections, AlprDetection bestDetection, int option) {
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

        boolean isSamePlate = (m_lastPlate != null && m_lastPlate.equalsIgnoreCase(bestDetection.getmPlate()));
        long skipMs = m_config.getM_timeSkipMs();
        long waitMs = m_config.getM_timeWaitMs();
        long elapsedMs = bestDetection.getTimestamp() - m_lastTimestamp;
        if (isSamePlate && elapsedMs < skipMs) {
            LOGGER.i("Skip " + m_lastPlate);
            if (m_callFromApp) {
                setResultAndExit();
            } else {
                readyForNextImage();
                m_actionButton.setText(R.string.button_label_stop);
            }
        } else {
            trackResult(true);

            AlprPlateInfo w_plateInfo = AlprStore.getInstance(appCtx).queryPlateinfo(bestDetection.getmPlate());
            if (w_plateInfo != null) {
                bestDetection.setInfo(w_plateInfo.getmInfo());
                notifyPlate(bestDetection.getmPlate(), w_plateInfo.getmInfo());
            } else {
                notifyPlate(bestDetection.getmPlate(), "");
            }

            AlprStore.getInstance(appCtx).insertDetection(bestDetection);
            m_lastPlate = bestDetection.getmPlate();
            m_lastTimestamp = bestDetection.getTimestamp();

            if (detections != null && detections.size() > option) {
                m_bestDetection = detections.get(option);
            }

            if (m_callFromApp) {
                setResultAndExit();
            }
            if ( m_functionMode == FUNCTION_MODE_MANUAL){
                readyForNextImage();
            } else {

                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    readyForNextImage();

                    m_actionButton.setText(R.string.button_label_stop);
                }, waitMs);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
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
        intent.putExtra("previewSizes", m_supportedSizes);

        onStop();
        m_configResultLauncher.launch(intent);
    }


    private void showPlatesInfo() {
        Intent intent = new Intent(this, PlatesInfoActivity.class);
        intent.putExtra("test", false);
        startActivity(intent);
    }

    private void showHelp() {
        String w_helpUrl = getResources().getString(R.string.help_url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(w_helpUrl));
        startActivity(browserIntent);
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
            trackResult(true);

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
                String w_titulo = String.format(Locale.getDefault(), "%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton4.setText(w_titulo);
                m_optButton4.setEnabled(true);
                m_optButton4.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 3) {
                AlprDetection detection = detections.get(2);
                String w_titulo = String.format(Locale.getDefault(), "%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton3.setText(w_titulo);
                m_optButton3.setEnabled(true);
                m_optButton3.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 2) {
                AlprDetection detection = detections.get(1);
                String w_titulo = String.format(Locale.getDefault(), "%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
                m_optButton2.setText(w_titulo);
                m_optButton2.setEnabled(true);
                m_optButton2.setVisibility(View.VISIBLE);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
            }
            if (detections.size() >= 1) {
                AlprDetection detection = detections.get(0);
                String w_titulo = String.format(Locale.getDefault(), "%s (%.2f)", detection.getmPlate(), detection.getmConfidence());
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
                    Toast.LENGTH_LONG).show();
        }
    }


    protected void notifyPlate(String plate, String info) {
        boolean w_textToSpeech = m_config.getM_textToSpeech();
        if (w_textToSpeech) {
            StringBuilder w_toSpeak = new StringBuilder(getResources().getString(R.string.lblPlate));
            for (int i = 0; i < plate.length(); i++) {
                w_toSpeak.append(plate.charAt(i)).append(" ");
            }

            if (info != null && !info.equalsIgnoreCase(""))
                w_toSpeak.append(info);
            else
                w_toSpeak.append(".");

            m_textToSpeech.speak(w_toSpeak.toString(), TextToSpeech.QUEUE_FLUSH, null, null);

        } else {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            if (info != null && !info.equalsIgnoreCase("")) {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 2000);

            } else {
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 200);

            }

        }
        String w_textPlate = getResources().getString(R.string.lblPlate) + " " + plate;
        if (info != null && !info.equalsIgnoreCase(""))
            w_textPlate += ": " + info;
        Toast.makeText(appCtx, w_textPlate,
                Toast.LENGTH_LONG).show();

    }


    protected void setResultAndExit() {
        Intent intent = new Intent();
        intent.putExtra("detected", (m_bestDetection != null));
        if (m_bestDetection == null) {
            setResult(RESULT_CANCELED, intent);
        } else {
            setResult(RESULT_OK, intent);
            intent.putExtra("plate", m_bestDetection.getmPlate());
            intent.putExtra("datetime", m_bestDetection.getmDatetime());
            intent.putExtra("confidence", m_bestDetection.getmConfidence());
            intent.putExtra("info", m_bestDetection.getInfo());
        }
        finish();
    }





    private void checkPreviewSizes(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        List<CameraInfo> availableCameraInfo = cameraProvider.getAvailableCameraInfos();

        LOGGER.i("[startCamera] available cameras:" + availableCameraInfo.size());
        List<Size> supportedSizesAll = new ArrayList<>();
        for (CameraInfo c : availableCameraInfo) {

            CameraSelector cameraSelector = c.getCameraSelector();

            androidx.camera.core.Camera cx = cameraProvider.bindToLifecycle(this, cameraSelector);

            @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"}) CameraCharacteristics camChars = Camera2CameraInfo
                    .extractCameraCharacteristics(cx.getCameraInfo());

            StreamConfigurationMap streamConfigurationMap = camChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            final List<Size> supportedSizes = Arrays.asList(sizes);
            supportedSizesAll.addAll(supportedSizes);
            LOGGER.i("Valid preview sizes: [" + TextUtils.join(", ", supportedSizes) + "]");
        }

        cameraProvider.unbindAll();

        List<String> sizesEntries = new ArrayList<>();

        for (Size s : supportedSizesAll) {
            int MIN_PREVIEW_SIZE_W = 720;
            int MIN_PREVIEW_SIZE_H = 720;
            if ( s.getWidth()< MIN_PREVIEW_SIZE_W || s.getHeight() < MIN_PREVIEW_SIZE_H)
                continue;

            if (!sizesEntries.contains(s.toString()))
                sizesEntries.add(s.toString());
        }
        Collections.sort(sizesEntries, (s, t1) -> {
            if (s.length() > t1.length()) {
                return s.compareTo("0"+t1);
            } else
            if (s.length() < t1.length()) {
                return ("0"+s).compareTo(t1);
            } else return s.compareTo(t1);
        });
        Object[] objectList = sizesEntries.toArray();
        m_supportedSizes = Arrays.copyOf(objectList, objectList.length, String[].class);

    }

    void setCropImage(){
        int sensorOrientation = 90 - getDisplayRotation();

        RectF cropRect = new RectF(0, 0, m_cropSize.getWidth(), m_cropSize.getHeight());
        cropToFrameTransform.mapRect(cropRect);

        Bitmap w_mutableBitmap = Bitmap.createBitmap(m_previewWidth, m_previewHeight, Bitmap.Config.ARGB_8888);
        cropImage.setImageBitmap(w_mutableBitmap);

        BoxTracker boxTracker = new BoxTracker();
        boxTracker.setFrameConfiguration(m_previewWidth, m_previewHeight, sensorOrientation);
        boxTracker.trackResult(null, w_mutableBitmap, null, cropRect, 0);
        boxTracker.draw(w_mutableBitmap);


    }

    protected void fillBytes(final ImageProxy.PlaneProxy[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }
}
