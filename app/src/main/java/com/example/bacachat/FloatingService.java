package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Locale;

public class FloatingService extends Service
        implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private View overlayBox;
    private View resizeHandle;
    private LinearLayout controlLayout;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams resizeParams;
    private WindowManager.LayoutParams controlParams;

    private TextToSpeech tts;

    private MediaProjection mediaProjection;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isReading = false;

    private static final int MIN_WIDTH = 250;
    private static final int MIN_HEIGHT = 180;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

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
                                getSystemService(MEDIA_PROJECTION_SERVICE);

                if (projectionManager != null) {
                    mediaProjection =
                            projectionManager.getMediaProjection(
                                    resultCode,
                                    data
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
                (WindowManager) getSystemService(WINDOW_SERVICE);

        tts = new TextToSpeech(this, this);

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

        boxParams = new WindowManager.LayoutParams(
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

        resizeParams = new WindowManager.LayoutParams(
                45,
                45,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        resizeParams.gravity = Gravity.TOP | Gravity.LEFT;

        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controlLayout.setPadding(
                10,
                5,
                10,
                5
        );

        Button btnRead = new Button(this);
        btnRead.setText("▶ Baca");

        Button btnLock = new Button(this);
        btnLock.setText("🔓 Lock");

        Button btnHide = new Button(this);
        btnHide.setText("👁 Sembunyi");

        Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnRead);
        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
        controlLayout.addView(btnClose);

        controlParams = new WindowManager.LayoutParams(
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

        } catch (Exception e) {

            e.printStackTrace();

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

                isReading = false;
                btnRead.setText("▶ Baca");

                Toast.makeText(
                        this,
                        "Pembacaan dihentikan.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                isReading = true;
                btnRead.setText("⏹ Stop");

                Toast.makeText(
                        this,
                        "Pembacaan siap.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        btnLock.setOnClickListener(v -> {

            isLocked = !isLocked;

            if (isLocked) {

                btnLock.setText("🔒 Unlock");
                resizeHandle.setVisibility(View.GONE);

            } else {

                btnLock.setText("🔓 Lock");
                resizeHandle.setVisibility(View.VISIBLE);
            }
        });

        btnHide.setOnClickListener(v -> {

            isHidden = !isHidden;

            if (isHidden) {

                overlayBox.setVisibility(View.GONE);
                resizeHandle.setVisibility(View.GONE);
                btnHide.setText("👁 Tampil");

            } else {

                overlayBox.setVisibility(View.VISIBLE);

                if (!isLocked) {
                    resizeHandle.setVisibility(View.VISIBLE);
                }

                btnHide.setText("👁 Sembunyi");
            }
        });

        btnClose.setOnClickListener(v -> stopSelf());
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

                                initialX = boxParams.x;
                                initialY = boxParams.y;

                                initialTouchX =
                                        event.getRawX();

                                initialTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                boxParams.x =
                                        initialX
                                                + (int)
                                                (event.getRawX()
                                                        - initialTouchX);

                                boxParams.y =
                                        initialY
                                                + (int)
                                                (event.getRawY()
                                                        - initialTouchY);

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
                                                (event.getRawX()
                                                        - initialTouchX);

                                int newHeight =
                                        initialHeight
                                                + (int)
                                                (event.getRawY()
                                                        - initialTouchY);

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
            e.printStackTrace();
        }
    }

    private void updateResizeHandle() {

        int screenWidth =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int screenHeight =
                getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        int centerX =
                screenWidth / 2 + boxParams.x;

        int centerY =
                screenHeight / 2 + boxParams.y;

        int left =
                centerX
                        + (boxParams.width / 2)
                        - (resizeParams.width / 2);

        int top =
                centerY
                        + (boxParams.height / 2)
                        - (resizeParams.height / 2);

        resizeParams.x = left;
        resizeParams.y = top;

        try {

            windowManager.updateViewLayout(
                    resizeHandle,
                    resizeParams
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            tts.setLanguage(
                    new Locale("id", "ID")
            );
        }
    }

    @Override
    public void onDestroy() {

        isReading = false;

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        if (windowManager != null) {

            try {
                if (overlayBox != null) {
                    windowManager.removeView(overlayBox);
                }
            } catch (Exception ignored) {
            }

            try {
                if (resizeHandle != null) {
                    windowManager.removeView(resizeHandle);
                }
            } catch (Exception ignored) {
            }

            try {
                if (controlLayout != null) {
                    windowManager.removeView(controlLayout);
                }
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
                }
