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
import android.util.Log;
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

    private static final String TAG = "BacaLivechat";

    private static final int MIN_WIDTH = 250;
    private static final int MIN_HEIGHT = 180;

    private WindowManager windowManager;

    private View overlayBox;
    private View resizeHandle;
    private LinearLayout controlLayout;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams resizeParams;
    private WindowManager.LayoutParams controlParams;

    private TextToSpeech tts;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private HandlerThread captureThread;
    private Handler captureHandler;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isReading = false;

    private boolean captureStarted = false;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent != null) {

            int resultCode = intent.getIntExtra(
                    "SCREEN_CAPTURE_RESULT_CODE",
                    -1
            );

            Intent data = intent.getParcelableExtra(
                    "SCREEN_CAPTURE_DATA"
            );

            if (resultCode != -1 && data != null) {

                MediaProjectionManager projectionManager =
                        (MediaProjectionManager)
                                getSystemService(
                                        MEDIA_PROJECTION_SERVICE
                                );

                if (projectionManager != null) {

                    mediaProjection =
                            projectionManager.getMediaProjection(
                                    resultCode,
                                    data
                            );

                    Log.d(
                            TAG,
                            "MediaProjection berhasil diterima."
                    );
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager)
                        getSystemService(WINDOW_SERVICE);

        tts = new TextToSpeech(
                this,
                this
        );

        screenWidth =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        screenHeight =
                getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        screenDensity =
                getResources()
                        .getDisplayMetrics()
                        .densityDpi;

        createOverlay();
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

        boxParams =
                new WindowManager.LayoutParams(
                        650,
                        450,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        boxParams.gravity = Gravity.CENTER;

        resizeHandle = new View(this);

        resizeHandle.setBackgroundColor(
                Color.parseColor("#AAFFFFFF")
        );

        resizeParams =
                new WindowManager.LayoutParams(
                        45,
                        45,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        resizeParams.gravity =
                Gravity.TOP | Gravity.LEFT;

        controlLayout =
                new LinearLayout(this);

        controlLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controlLayout.setPadding(
                10,
                5,
                10,
                5
        );

        Button btnRead =
                new Button(this);

        btnRead.setText("▶ Baca");

        Button btnLock =
                new Button(this);

        btnLock.setText("🔓 Lock");

        Button btnHide =
                new Button(this);

        btnHide.setText("👁 Sembunyi");

        Button btnClose =
                new Button(this);

        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnRead);
        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
        controlLayout.addView(btnClose);

        controlParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        controlParams.gravity =
                Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        controlParams.y = 80;

        try {

            windowManager.addView(
                    overlayBox,
                    boxParams
            );

            windowManager.addView(
                    resizeHandle,
                    resizeParams
            );

            windowManager.addView(
                    controlLayout,
                    controlParams
            );

            updateResizeHandle();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Gagal membuat overlay.",
                    e
            );

            Toast.makeText(
                    this,
                    "Gagal menampilkan pembaca.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        setupDrag();
        setupResize();

        btnRead.setOnClickListener(v -> {

            if (isReading) {

                stopScreenCapture();

                isReading = false;

                btnRead.setText("▶ Baca");

                Toast.makeText(
                        this,
                        "Pembacaan dihentikan.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                if (mediaProjection == null) {

                    Toast.makeText(
                            this,
                            "Izin menangkap layar belum tersedia.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                startScreenCapture();

                isReading = true;

                btnRead.setText("⏹ Stop");

                Toast.makeText(
                        this,
                        "Screen capture aktif.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        btnLock.setOnClickListener(v -> {

            isLocked = !isLocked;

            if (isLocked) {

                btnLock.setText("🔒 Unlock");

                resizeHandle.setVisibility(
                        View.GONE
                );

            } else {

                btnLock.setText("🔓 Lock");

                if (!isHidden) {

                    resizeHandle.setVisibility(
                            View.VISIBLE
                    );
                }
            }
        });

        btnHide.setOnClickListener(v -> {

            isHidden = !isHidden;

            if (isHidden) {

                overlayBox.setVisibility(
                        View.GONE
                );

                resizeHandle.setVisibility(
                        View.GONE
                );

                btnHide.setText("👁 Tampil");

            } else {

                overlayBox.setVisibility(
                        View.VISIBLE
                );

                if (!isLocked) {

                    resizeHandle.setVisibility(
                            View.VISIBLE
                    );
                }

                btnHide.setText("👁 Sembunyi");
            }
        });

        btnClose.setOnClickListener(
                v -> stopSelf()
        );
    }

    private void setupDrag() {

        overlayBox.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialX;
                    private int initialY;

                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

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
                                                + (int)
                                                (
                                                        event.getRawX()
                                                                - initialTouchX
                                                );

                                boxParams.y =
                                        initialY
                                                + (int)
                                                (
                                                        event.getRawY()
                                                                - initialTouchY
                                                );

                                updateOverlay();

                                return true;
                        }

                        return false;
                    }
                }
        );
    }

    private void setupResize() {

        resizeHandle.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialWidth;
                    private int initialHeight;

                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

                        if (isLocked) {
                            return false;
                        }

                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                initialWidth =
                                        boxParams.width;

                                initialHeight =
                                        boxParams.height;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                int newWidth =
                                        initialWidth
                                                + (int)
                                                (
                                                        event.getRawX()
                                                                - initialTouchX
                                                );

                                int newHeight =
                                        initialHeight
                                                + (int)
                                                (
                                                        event.getRawY()
                                                                - initialTouchY
                                                );

                                boxParams.width =
                                        Math.max(
                                                MIN_WIDTH,
                                                newWidth
                                        );

                                boxParams.height =
                                        Math.max(
                                                MIN_HEIGHT,
                                                newHeight
                                        );

                                updateOverlay();

                                return true;
                        }

                        return false;
                    }
                }
        );
    }

    private void updateOverlay() {

        try {

            windowManager.updateViewLayout(
                    overlayBox,
                    boxParams
            );

            updateResizeHandle();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Gagal memperbarui overlay.",
                    e
            );
        }
    }

    private void updateResizeHandle() {

        int centerX =
                screenWidth / 2
                        + boxParams.x;

        int centerY =
                screenHeight / 2
                        + boxParams.y;

        int left =
                centerX
                        + boxParams.width / 2
                        - resizeParams.width / 2;

        int top =
                centerY
                        + boxParams.height / 2
                        - resizeParams.height / 2;

        resizeParams.x = left;
        resizeParams.y = top;

        try {

            windowManager.updateViewLayout(
                    resizeHandle,
                    resizeParams
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Gagal memindahkan resize handle.",
                    e
            );
        }
    }

    private void startScreenCapture() {

        if (captureStarted) {
            return;
        }

        if (mediaProjection == null) {
            return;
        }

        captureThread =
                new HandlerThread(
                        "ScreenCaptureThread"
                );

        captureThread.start();

        captureHandler =
                new Handler(
                        captureThread.getLooper()
                );

        imageReader =
                ImageReader.newInstance(
                        screenWidth,
                        screenHeight,
                        android.graphics.PixelFormat.RGBA_8888,
                        2
                );

        imageReader.setOnImageAvailableListener(
                reader -> processLatestFrame(reader),
                captureHandler
        );

        virtualDisplay =
                mediaProjection.createVirtualDisplay(
                        "BacaLivechatCapture",
                        screenWidth,
                        screenHeight,
                        screenDensity,
                        0,
                        imageReader.getSurface(),
                        null,
                        captureHandler
                );

        captureStarted = true;

        Log.d(
                TAG,
                "VirtualDisplay berhasil dibuat."
        );
    }

    private void processLatestFrame(
            ImageReader reader
    ) {

        Image image = null;

        try {

            image = reader.acquireLatestImage();

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

            Bitmap fullBitmap =
                    Bitmap.createBitmap(
                            bitmapWidth,
                            screenHeight,
                            Bitmap.Config.ARGB_8888
                    );

            buffer.rewind();

            fullBitmap.copyPixelsFromBuffer(
                    buffer
            );

            int cropLeft =
                    screenWidth / 2
                            + boxParams.x
                            - boxParams.width / 2;

            int cropTop =
                    screenHeight / 2
                            + boxParams.y
                            - boxParams.height / 2;

            cropLeft =
                    Math.max(
                            0,
                            cropLeft
                    );

            cropTop =
                    Math.max(
                            0,
                            cropTop
                    );

            int cropWidth =
                    Math.min(
                            boxParams.width,
                            fullBitmap.getWidth()
                                    - cropLeft
                    );

            int cropHeight =
                    Math.min(
                            boxParams.height,
                            fullBitmap.getHeight()
                                    - cropTop
                    );

            if (cropWidth > 0
                    && cropHeight > 0) {

                Bitmap croppedBitmap =
                        Bitmap.createBitmap(
                                fullBitmap,
                                cropLeft,
                  
