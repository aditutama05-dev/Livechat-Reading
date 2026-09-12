package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Locale;

public class FloatingService extends Service
        implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private View overlayBox;
    private LinearLayout controlLayout;

    private TextToSpeech tts;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isCapturing = false;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private HandlerThread captureThread;
    private Handler captureHandler;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private WindowManager.LayoutParams boxParams;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        tts = new TextToSpeech(this, this);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        createOverlay();
        createCaptureThread();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent != null) {

            int resultCode = intent.getIntExtra(
                    "SCREEN_CAPTURE_RESULT_CODE",
                    -1
            );

            Intent captureData = null;

            if (Build.VERSION.SDK_INT >= 33) {
                captureData = intent.getParcelableExtra(
                        "SCREEN_CAPTURE_DATA",
                        Intent.class
                );
            } else {
                captureData = intent.getParcelableExtra(
                        "SCREEN_CAPTURE_DATA"
                );
            }

            if (resultCode != -1 && captureData != null) {
                setupMediaProjection(
                        resultCode,
                        captureData
                );
            }
        }

        return START_STICKY;
    }

    private void createOverlay() {

        int layoutType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        overlayBox = new View(this);

        overlayBox.setBackgroundColor(
                Color.parseColor("#3300FF88")
        );

        boxParams = new WindowManager.LayoutParams(
                650,
                450,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        boxParams.gravity = Gravity.CENTER;

        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controlLayout.setPadding(
                10,
                10,
                10,
                10
        );

        Button btnLock = new Button(this);
        btnLock.setText("🔒 Lock");

        Button btnHide = new Button(this);
        btnHide.setText("👁️ Sembunyi");

        Button btnRead = new Button(this);
        btnRead.setText("📖 Baca");

        Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
        controlLayout.addView(btnRead);
        controlLayout.addView(btnClose);

        WindowManager.LayoutParams controlParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        controlParams.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        controlParams.y = 120;

        try {

            windowManager.addView(
                    overlayBox,
                    boxParams
            );

            windowManager.addView(
                    controlLayout,
                    controlParams
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Gagal membuat overlay.",
                    Toast.LENGTH_LONG
            ).show();
        }

        overlayBox.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialX;
                    private int initialY;

                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event) {

                        if (isLocked) {
                            return false;
                        }

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialX =
                                        boxParams.x;

                                initialY =
                                        boxParams.y;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                boxParams.x =
                                        initialX
                                                + (int) (
                                                event.getRawX()
                                                        - initialTouchX
                                        );

                                boxParams.y =
                                        initialY
                                                + (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );

                                try {

                                    windowManager.updateViewLayout(
                                            overlayBox,
                                            boxParams
                                    );

                                } catch (Exception ignored) {
                                }

                                return true;
                        }

                        return false;
                    }
                }
        );

        btnLock.setOnClickListener(
                v -> {

                    isLocked = !isLocked;

                    btnLock.setText(
                            isLocked
                                    ? "🔒 Terkunci"
                                    : "🔓 Lock"
                    );

                    Toast.makeText(
                            FloatingService.this,
                            isLocked
                                    ? "Kotak terkunci."
                                    : "Kotak bisa digeser.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        btnHide.setOnClickListener(
                v -> {

                    isHidden = !isHidden;

                    overlayBox.setVisibility(
                            isHidden
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    btnHide.setText(
                            isHidden
                                    ? "👁️ Tampil"
                                    : "👁️ Sembunyi"
                    );
                }
        );

        btnRead.setOnClickListener(
                v -> {

                    if (!isCapturing) {

                        startScreenCapture();

                        Toast.makeText(
                                FloatingService.this,
                                "Pembacaan layar aktif.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        stopScreenCapture();

                        Toast.makeText(
                                FloatingService.this,
                                "Pembacaan layar berhenti.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        btnClose.setOnClickListener(
                v -> stopSelf()
        );
    }

    private void createCaptureThread() {

        captureThread =
                new HandlerThread(
                        "LiveChatCaptureThread"
                );

        captureThread.start();

        captureHandler =
                new Handler(
                        captureThread.getLooper()
                );
    }

    private void setupMediaProjection(
            int resultCode,
            Intent captureData) {

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        if (projectionManager == null) {
            return;
        }

        try {

            mediaProjection =
                    projectionManager.getMediaProjection(
                            resultCode,
                            captureData
                    );

            if (mediaProjection == null) {
                return;
            }

            mediaProjection.registerCallback(
                    new MediaProjection.Callback() {

                        @Override
                        public void onStop() {
                            stopScreenCapture();
                        }
                    },
                    captureHandler
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void startScreenCapture() {

        if (mediaProjection == null) {

            Toast.makeText(
                    this,
                    "Izin menangkap layar belum tersedia.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (isCapturing) {
            return;
        }

        try {

            imageReader =
                    ImageReader.newInstance(
                            screenWidth,
                            screenHeight,
                            PixelFormat.RGBA_8888,
                            2
                    );

            virtualDisplay =
                    mediaProjection.createVirtualDisplay(
                            "LiveChatReader",
                            screenWidth,
                            screenHeight,
                            screenDensity,
                            0,
                            imageReader.getSurface(),
                            null,
                            captureHandler
                    );

            imageReader.setOnImageAvailableListener(
                    reader -> processLatestFrame(),
                    captureHandler
            );

            isCapturing = true;

        } catch (Exception e) {

            e.printStackTrace();

            stopScreenCapture();
        }
    }

    private void processLatestFrame() {

        if (imageReader == null) {
            return;
        }

        Image image = null;
        Bitmap fullBitmap = null;
        Bitmap croppedBitmap = null;

        try {

            image =
                    imageReader.acquireLatestImage();

            if (image == null) {
                return;
            }

            Image.Plane[] planes =
                    image.getPlanes();

            if (planes.length == 0) {
                return;
            }

            Image.Plane plane =
                    planes[0];

            ByteBuffer buffer =
                    plane.getBuffer();

            int pixelStride =
                    plane.getPixelStride();

            int rowStride =
                    plane.getRowStride();

            int rowPadding =
                    rowStride
                            - pixelStride * screenWidth;

            int bitmapWidth =
                    screenWidth
                            + rowPadding / pixelStride;

            fullBitmap =
                    Bitmap.createBitmap(
                            bitmapWidth,
                            screenHeight,
                            Bitmap.Config.ARGB_8888
                    );

            buffer.rewind();

            fullBitmap.copyPixelsFromBuffer(
                    buffer
            );

            int cropWidth =
                    Math.min(
                            boxParams.width,
                            screenWidth
                    );

            int cropHeight =
                    Math.min(
                            boxParams.height,
                            screenHeight
                    );

            int cropX =
                    screenWidth / 2
                            + boxParams.x
                            - cropWidth / 2;

            int cropY =
                    screenHeight / 2
                            + boxParams.y
                            - cropHeight / 2;

            cropX =
                    Math.max(
                            0,
                            Math.min(
                                    cropX,
                                    bitmapWidth - cropWidth
                            )
                    );

            cropY =
                    Math.max(
                            0,
                            Math.min(
                                    cropY,
                                    screenHeight - cropHeight
                            )
                    );

            croppedBitmap =
                    Bitmap.createBitmap(
                            fullBitmap,
                            cropX,
                            cropY,
                            cropWidth,
                            cropHeight
                    );

            /*
             * TEMPORARY FRAME ONLY.
             *
             * Bitmap digunakan di RAM.
             * Tidak disimpan ke penyimpanan.
             *
             * Tahap berikutnya:
             * croppedBitmap akan dikirim ke OCR.
             */

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (croppedBitmap != null) {
                croppedBitmap.recycle();
            }

            if (fullBitmap != null) {
                fullBitmap.recycle();
            }

            if (image != null) {
                image.close();
            }
        }
    }

    private void stopScreenCapture() {

        isCapturing = false;

        if (virtualDisplay != null) {

            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {

            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            Locale defaultLocale =
                    Locale.getDefault();

            int result =
                    tts.setLanguage(
                            defaultLocale
                    );

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                tts.setLanguage(
                        Locale.ENGLISH
                );
            }
        }
    }

    @Override
    public void onDestroy() {

        stopScreenCapture();

        if (mediaProjection != null) {

            mediaProjection.stop();
            mediaProjection = null;
        }

        if (captureThread != null) {

            captureThread.quitSafely();
            captureThread = null;
        }

        if (tts != null) {

            tts.stop();
            tts.shutdown();
            tts = null;
        }

        try {

            if (overlayBox != null) {
                windowManager.removeView(overlayBox);
                overlayBox = null;
            }

            if (controlLayout != null) {
                windowManager.removeView(controlLayout);
                controlLayout = null;
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                    }
