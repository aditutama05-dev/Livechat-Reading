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
import android.view.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Locale;

public class FloatingService extends Service implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private View overlayBox;
    private LinearLayout controlLayout;

    private TextToSpeech tts;
    private LanguageSettings languageSettings;

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

    private int captureResultCode;
    private Intent captureData;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        languageSettings = new LanguageSettings(this);

        tts = new TextToSpeech(this, this);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        captureThread = new HandlerThread("LiveChatCaptureThread");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());

        createOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {

            captureResultCode = intent.getIntExtra(
                    "SCREEN_CAPTURE_RESULT_CODE",
                    -1
            );

            captureData = intent.getParcelableExtra(
                    "SCREEN_CAPTURE_DATA"
            );

            if (captureResultCode != -1 && captureData != null) {
                setupMediaProjection();
            }
        }

        return
