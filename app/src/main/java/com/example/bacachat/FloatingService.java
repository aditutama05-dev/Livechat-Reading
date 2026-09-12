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

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.Locale;

public class FloatingService extends Service
        implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private View overlayBox;
    private LinearLayout controlLayout;

    private WindowManager.LayoutParams boxParams;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isCapturing = false;
    private boolean isOcrRunning = false;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private HandlerThread captureThread;
    private Handler captureHandler;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private int captureResultCode = -1;
    private Intent captureData;

    private TextToSpeech tts;

    private TextRecognizer latinRecognizer;
    private TextRecognizer chineseRecognizer;
    private TextRecognizer devanagariRecognizer;
    private TextRecognizer japaneseRecognizer;
    private TextRecognizer koreanRecognizer;

    private LanguageIdentifier languageIdentifier;

    private String lastSpokenText = "";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        windowManager
                .getDefaultDisplay()
                .getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        captureThread =
                new HandlerThread("LiveChatCapture");

        captureThread.start();

        captureHandler =
                new Handler(captureThread.getLooper());

        tts =
                new TextToSpeech(
                        this,
                        this
                );

        latinRecognizer =
                TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                );

        chineseRecognizer =
                TextRecognition.getClient(
                        ChineseTextRecognizerOptions
                                .Builder()
                                .build()
                );

        devanagariRecognizer =
                TextRecognition.getClient(
                        DevanagariTextRecognizerOptions
                                .Builder()
                                .build()
                );

        japaneseRecognizer =
                TextRecognition.getClient(
                        JapaneseTextRecognizerOptions
                                .Builder()
                                .build()
                );

        koreanRecognizer =
                TextRecognition.getClient(
                        KoreanTextRecognizerOptions
                                .Builder()
                                .build()
                );

        languageIdentifier =
                LanguageIdentification
                        .getClient();

        createOverlay();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent != null) {

            captureResultCode =
                    intent.getIntExtra(
                            "SCREEN_CAPTURE_RESULT_CODE",
                            -1
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU) {

                captureData =
                        intent.getParcelableExtra(
                                "SCREEN_CAPTURE_DATA",
                                Intent.class
                        );

            } else {

                captureData =
                        intent.getParcelableExtra(
                                "SCREEN_CAPTURE_DATA"
                        );
            }

            if (captureResultCode != -1 &&
                    captureData != null) {

                setupMediaProjection();
            }
        }

        return START_STICKY;
    }

    private void createOverlay() {

        int overlayType;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            overlayType =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            overlayType =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        overlayBox =
                new View(this);

        overlayBox.setBackgroundColor(
                Color.parseColor("#3300FF88")
        );

        boxParams =
                new WindowManager.LayoutParams(
                        650,
                        450,
                        overlayType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        boxParams.gravity =
                Gravity.CENTER;

        controlLayout =
                new LinearLayout(this);

        controlLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button btnLock =
                new Button(this);

        btnLock.setText("🔒 Lock");

        Button btnHide =
                new Button(this);

        btnHide.setText("👁️ Sembunyi");

        Button btnRead =
                new Button(this);

        btnRead.setText("🔊 Baca");

        Button btnClose =
                new Button(this);

        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
        controlLayout.addView(btnRead);
        controlLayout.addView(btnClose);

        WindowManager.LayoutParams controlParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        WindowManager.LayoutParams
                                .WRAP_CONTENT,
                        overlayType,
                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        controlParams.gravity =
                Gravity.TOP |
                        Gravity.CENTER_HORIZONTAL;

        controlParams.y = 80;

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

            Toast.makeText(
                    this,
                    "Gagal membuat overlay",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        setupDragging();

        btnLock.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        isLocked =
                                !isLocked;

                        if (isLocked) {

                            btnLock.setText(
                                    "🔒 Terkunci"
                            );

                            Toast.makeText(
                                    FloatingService.this,
                                    "Area live chat dikunci",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            btnLock.setText(
                                    "🔓 Lock"
                            );

                            Toast.makeText(
                                    FloatingService.this,
                                    "Area live chat bisa digeser",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );

        btnHide.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        isHidden =
                                !isHidden;

                        if (isHidden) {

                            overlayBox.setVisibility(
                                    View.GONE
                            );

                            btnHide.setText(
                                    "👁️ Tampil"
                            );

                        } else {

                            overlayBox.setVisibility(
                                    View.VISIBLE
                            );

                            btnHide.setText(
                                    "👁️ Sembunyi"
                            );
                        }
                    }
                }
        );

        btnRead.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (!isCapturing) {

                            Toast.makeText(
                                    FloatingService.this,
                                    "Screen capture belum aktif",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        if (isOcrRunning) {

                            return;
                        }

                        captureCurrentFrame();
                    }
                }
        );

        btnClose.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        stopSelf();
                    }
                }
        );
    }

    private void setupDragging() {

        overlayBox.setOnTouchListener(
                new View.OnTouchListener() {

                    private int initialX;
                    private int initialY;

                    private float initialTouchX;
                    private float initialTouchY;

                    @Override
                    public boolean onTouch(
                            View view,
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

                                    windowManager
                                            .updateViewLayout(
                                                    overlayBox,
                                                    boxParams
                                            );

                                } catch (Exception ignored) {
                                }

                                return true;

                            case MotionEvent.ACTION_UP:

                                return true;

                            default:

                                return false;
                        }
                    }
                }
        );
    }

    private void setupMediaProjection() {

        try {

            MediaProjectionManager manager =
                    (MediaProjectionManager)
                            getSystemService(
                                    MEDIA_PROJECTION_SERVICE
                            );

            if (manager == null) {
                return;
            }

            mediaProjection =
                    manager.getMediaProjection(
                            captureResultCode,
                            captureData
                    );

            if (mediaProjection == null) {

                Toast.makeText(
                        this,
                        "MediaProjection gagal",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            startScreenCapture();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal memulai screen capture",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void startScreenCapture() {

        if (mediaProjection == null) {
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

            imageReader.setOnImageAvailableListener(
                    new ImageReader.OnImageAvailableListener() {

                        @Override
                        public void onImageAvailable(
                                ImageReader reader
                        ) {

                            Image image = null;

                            try {

                                image =
                                        reader
                                                .acquireLatestImage();

                                if (image == null) {
                                    return;
                                }

                                processFrame(
                                        image,
                                        false
                                );

                            } catch (Exception ignored) {

                                if (image != null) {
                                    image.close();
                                }
                            }
                        }
                    },
                    captureHandler
            );

            virtualDisplay =
                    mediaProjection
                            .createVirtualDisplay(
                                    "BacaLivechat",
                                    screenWidth,
                                    screenHeight,
                                    screenDensity,
                                    0,
                                    imageReader.getSurface(),
                                    null,
                                    captureHandler
                            );

            isCapturing = true;

            Toast.makeText(
                    this,
                    "Screen capture aktif",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            isCapturing = false;

            Toast.makeText(
                    this,
                    "Screen capture gagal",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void captureCurrentFrame() {

        if (imageReader == null) {
            return;
        }

        Image image = null;

        try {

            image =
                    imageReader
                            .acquireLatestImage();

            if (image == null) {

                Toast.makeText(
                        this,
                        "Belum ada frame layar",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            processFrame(
                    image,
                    true
            );

        } catch (Exception e) {

            if (image != null) {
                image.close();
            }
        }
    }

    private void processFrame(
            Image image,
            boolean runOcr
    ) {

        Bitmap fullBitmap = null;
        Bitmap croppedBitmap = null;

        try {

            if (!runOcr) {
                return;
            }

            if (isOcrRunning) {
                return;
            }

            Image.Plane[] planes =
                    image.getPlanes();

            if (planes == null ||
                    planes.length == 0) {
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
                    rowStride -
                            pixelStride *
                                    screenWidth;

            int bitmapWidth =
                    screenWidth +
                            rowPadding /
                                    pixelStride;

            buffer.rewind();

            fullBitmap =
                    Bitmap.createBitmap(
                            bitmapWidth,
                            screenHeight,
                            Bitmap.Config.A
