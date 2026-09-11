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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.Locale;

public class FloatingService extends Service
        implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private View overlayBox;
    private View resizeHandle;
    private LinearLayout controlLayout;

    private TextToSpeech tts;

    private TextRecognizer textRecognizer;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isReading = false;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams resizeParams;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private static final int MIN_BOX_WIDTH = 250;
    private static final int MIN_BOX_HEIGHT = 150;

    // =========================
    // SCREEN CAPTURE
    // =========================

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private HandlerThread captureThread;
    private Handler captureHandler;

    private int captureWidth;
    private int captureHeight;

    // =========================
    // OCR
    // =========================

    private boolean ocrBusy = false;

    private String lastSpokenText = "";

    private long lastReadTime = 0;

    private static final long MIN_READ_INTERVAL = 1200;

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

            int resultCode =
                    intent.getIntExtra(
                            "SCREEN_CAPTURE_RESULT_CODE",
                            -1
                    );

            Intent captureData =
                    intent.getParcelableExtra(
                            "SCREEN_CAPTURE_DATA"
                    );

            if (resultCode != -1 && captureData != null) {

                startScreenCapture(
                        resultCode,
                        captureData
                );
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

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        windowManager
                .getDefaultDisplay()
                .getRealMetrics(metrics);

        screenWidth =
                metrics.widthPixels;

        screenHeight =
                metrics.heightPixels;

        screenDensity =
                metrics.densityDpi;

        // =========================
        // TTS
        // =========================

        tts =
                new TextToSpeech(
                        this,
                        this
                );

        // =========================
        // OCR
        // =========================

        textRecognizer =
                TextRecognition
                        .getClient(
                                TextRecognizerOptions
                                        .DEFAULT_OPTIONS
                        );

        // =========================
        // OVERLAY TYPE
        // =========================

        int layoutType;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            layoutType =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            layoutType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        // =========================
        // AREA LIVE CHAT
        // =========================

        overlayBox =
                new View(this);

        overlayBox.setBackgroundColor(
                Color.parseColor(
                        "#3300FF88"
                )
        );

        boxParams =
                new WindowManager.LayoutParams(
                        650,
                        450,
                        layoutType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        boxParams.gravity =
                Gravity.CENTER;

        // =========================
        // RESIZE HANDLE
        // =========================

        resizeHandle =
                new View(this);

        resizeHandle.setBackgroundColor(
                Color.WHITE
        );

        resizeParams =
                new WindowManager.LayoutParams(
                        40,
                        40,
                        layoutType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        // =========================
        // CONTROL
        // =========================

        controlLayout =
                new LinearLayout(this);

        controlLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button btnRead =
                new Button(this);

        btnRead.setText(
                "▶️ Baca"
        );

        Button btnLock =
                new Button(this);

        btnLock.setText(
                "🔓 Lock"
        );

        Button btnHide =
                new Button(this);

        btnHide.setText(
                "👁️ Sembunyi"
        );

        Button btnClose =
                new Button(this);

        btnClose.setText(
                "❌ Tutup"
        );

        controlLayout.addView(
                btnRead
        );

        controlLayout.addView(
                btnLock
        );

        controlLayout.addView(
                btnHide
        );

        controlLayout.addView(
                btnClose
        );

        WindowManager.LayoutParams
                controlParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        layoutType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        controlParams.gravity =
                Gravity.TOP |
                        Gravity.CENTER_HORIZONTAL;

        controlParams.y = 100;

        // =========================
        // TAMBAHKAN OVERLAY
        // =========================

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

            e.printStackTrace();
        }

        // =========================
        // DRAG KOTAK
        // =========================

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

                        switch (
                                event.getAction()
                        ) {

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
                                        initialX +
                                                (int) (
                                                        event.getRawX()
                                                                - initialTouchX
                                                );

                                boxParams.y =
                                        initialY +
                                                (int) (
                                                        event.getRawY()
                                                                - initialTouchY
                                                );

                                updateBox();

                                return true;
                        }

                        return false;
                    }
                }
        );

        // =========================
        // RESIZE
        // =========================

        resizeHandle.setOnTouchListener(
                new View.OnTouchListener() {

                    private int startWidth;
                    private int startHeight;

                    private float startX;
                    private float startY;

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    ) {

                        if (isLocked) {
                            return false;
                        }

                        switch (
                                event.getAction()
                        ) {

                            case MotionEvent.ACTION_DOWN:

                                startWidth =
                                        boxParams.width;

                                startHeight =
                                        boxParams.height;

                                startX =
                                        event.getRawX();

                                startY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                int newWidth =
                                        startWidth +
                                                (int) (
                                                        event.getRawX()
                                                                - startX
                                                );

                                int newHeight =
                                        startHeight +
                                                (int) (
                                                        event.getRawY()
                                                                - startY
                                                );

                                boxParams.width =
                                        Math.max(
                                                MIN_BOX_WIDTH,
                                                newWidth
                                        );

                                boxParams.height =
                                        Math.max(
                                                MIN_BOX_HEIGHT,
                                                newHeight
                                        );

                                updateBox();

                                return true;
                        }

                        return false;
                    }
                }
        );

        // =========================
        // BACA
        // =========================

        btnRead.setOnClickListener(
                v -> {

                    if (isReading) {

                        isReading =
                                false;

                        btnRead.setText(
                                "▶️ Baca"
                        );

                        if (tts != null) {
                            tts.stop();
                        }

                    } else {

                        if (mediaProjection == null) {

                            Toast.makeText(
                                    FloatingService.this,
                                    "Screen capture belum aktif",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        isReading =
                                true;

                        lastSpokenText =
                                "";

                        btnRead.setText(
                                "⏸️ Berhenti"
                        );

                        Toast.makeText(
                                FloatingService.this,
                                "Pembacaan live chat aktif",
                                Toast.LENGTH_SHORT
                        ).show();

                        captureCurrentFrame();
                    }
                }
        );

        // =========================
        // LOCK
        // =========================

        btnLock.setOnClickListener(
                v -> {

                    isLocked =
                            !isLocked;

                    btnLock.setText(
                            isLocked
                                    ? "🔒 Terkunci"
                                    : "🔓 Lock"
                    );

                    Toast.makeText(
                            FloatingService.this,
                            isLocked
                                    ? "Kotak terkunci"
                                    : "Kotak dapat digeser dan diubah ukurannya",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        // =========================
        // HIDE
        // =========================

        btnHide.setOnClickListener(
                v -> {

                    isHidden =
                            !isHidden;

                    overlayBox.setVisibility(
                            isHidden
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    resizeHandle.setVisibility(
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

        // =========================
        // CLOSE
        // =========================

        btnClose.setOnClickListener(
                v -> stopSelf()
        );
    }

    // =====================================================
    // SCREEN CAPTURE
    // =====================================================

    private void startScreenCapture(
            int resultCode,
            Intent data
    ) {

        stopScreenCapture();

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        if (projectionManager == null) {
            return;
        }

        mediaProjection =
                projectionManager.getMediaProjection(
                        resultCode,
                        data
                );

        if (mediaProjection == null) {
            return;
        }

        captureWidth =
                screenWidth;

        captureHeight =
                screenHeight;

        captureThread =
                new HandlerThread(
                        "LiveChatCapture"
                );

        captureThread.start();

        captureHandler =
                new Handler(
                        captureThread.getLooper()
                );

        imageReader =
                ImageReader.newInstance(
                        captureWidth,
                        captureHeight,
                        PixelFormat.RGBA_8888,
                        2
                );

        virtualDisplay =
                mediaProjection.createVirtualDisplay(
                        "BacaLivechat",
                        captureWidth,
                        captureHeight,
                        screenDensity,
                        android.hardware.display
                                .DisplayManager
                                .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(),
                        null,
                        captureHandler
                );

        imageReader.setOnImageAvailableListener(
                reader -> {

                    Image image = null;

                    try {

                        image =
                                reader.acquireLatestImage();

                        if (image == null) {
                            return;
                        }

                        if (!isReading) {
                            return;
                        }

                        if (ocrBusy) {
                            return;
                        }

                        long now =
                                System.currentTimeMillis();

                        if (now - lastReadTime <
                                MIN_READ_INTERVAL) {

                            return;
                        }

                        ocrBusy = true;

                        lastReadTime =
                                now;

                        processFrameInMemory(
                                image
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                        ocrBusy = false;

                    } finally {

                        if (image != null) {
                            image.close();
                        }
           
