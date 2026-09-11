package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
    private TextToSpeech tts;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams resizeParams;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isReading = false;

    private static final int MIN_WIDTH = 250;
    private static final int MIN_HEIGHT = 150;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        tts = new TextToSpeech(this, this);

        int layoutType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        // ========================================
        // KOTAK AREA LIVE CHAT
        // ========================================

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

        // ========================================
        // HANDLE RESIZE
        // ========================================

        resizeHandle = new View(this);

        resizeHandle.setBackgroundColor(
                Color.parseColor("#FF00AA88")
        );

        resizeParams = new WindowManager.LayoutParams(
                60,
                60,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        resizeParams.gravity = Gravity.CENTER;

        // ========================================
        // PANEL KONTROL
        // ========================================

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
        btnRead.setText("▶️ Baca");

        Button btnLock = new Button(this);
        btnLock.setText("🔓 Lock");

        Button btnHide = new Button(this);
        btnHide.setText("👁️ Sembunyi");

        Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnRead);
        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
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

        // ========================================
        // TAMPILKAN OVERLAY
        // ========================================

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
                    "Gagal menampilkan overlay",
                    Toast.LENGTH_SHORT
            ).show();
        }

        // ========================================
        // GESER KOTAK
        // ========================================

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

                                updateOverlayPosition();

                                return true;
                        }

                        return false;
                    }
                }
        );

        // ========================================
        // RESIZE
        // ========================================

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
                                        initialWidth +
                                        (int) (
                                                event.getRawX()
                                                        - initialTouchX
                                        );

                                int newHeight =
                                        initialHeight +
                                        (int) (
                                                event.getRawY()
                                                        - initialTouchY
                                        );

                                if (newWidth >= MIN_WIDTH) {
                                    boxParams.width =
                                            newWidth;
                                }

                                if (newHeight >= MIN_HEIGHT) {
                                    boxParams.height =
                                            newHeight;
                                }

                                updateOverlayPosition();

                                return true;
                        }

                        return false;
                    }
                }
        );

        // ========================================
        // BACA / PAUSE
        // ========================================

        btnRead.setOnClickListener(v -> {

            isReading = !isReading;

            if (isReading) {

                btnRead.setText("⏸️ Pause");

                Toast.makeText(
                        FloatingService.this,
                        "Pembacaan aktif",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                btnRead.setText("▶️ Baca");

                if (tts != null) {
                    tts.stop();
                }

                Toast.makeText(
                        FloatingService.this,
                        "Pembacaan dijeda",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ========================================
        // LOCK
        // ========================================

        btnLock.setOnClickListener(v -> {

            isLocked = !isLocked;

            if (isLocked) {

                btnLock.setText("🔒 Terkunci");

                resizeHandle.setVisibility(
                        View.GONE
                );

                Toast.makeText(
                        FloatingService.this,
                        "Area live chat dikunci",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                btnLock.setText("🔓 Lock");

                if (!isHidden) {
                    resizeHandle.setVisibility(
                            View.VISIBLE
                    );
                }

                Toast.makeText(
                        FloatingService.this,
                        "Area bisa digeser dan diubah ukurannya",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ========================================
        // SEMBUNYIKAN / TAMPILKAN
        // ========================================

        btnHide.setOnClickListener(v -> {

            isHidden = !isHidden;

            if (isHidden) {

                overlayBox.setVisibility(
                        View.GONE
                );

                resizeHandle.setVisibility(
                        View.GONE
                );

                btnHide.setText("👁️ Tampil");

            } else {

                overlayBox.setVisibility(
                        View.VISIBLE
                );

                if (!isLocked) {
                    resizeHandle.setVisibility(
                            View.VISIBLE
                    );
                }

                btnHide.setText(
                        "👁️ Sembunyi"
                );
            }
        });

        // ========================================
        // TUTUP
        // ========================================

        btnClose.setOnClickListener(v -> {
            stopSelf();
        });

        updateOverlayPosition();
    }

    // ============================================
    // POSISI HANDLE RESIZE
    // ============================================

    private void updateOverlayPosition() {

        if (boxParams == null ||
                resizeParams == null) {
            return;
        }

        resizeParams.x =
                boxParams.x +
                (boxParams.width / 2) -
                30;

        resizeParams.y =
                boxParams.y +
                (boxParams.height / 2) -
                30;

        try {

            windowManager.updateViewLayout(
                    overlayBox,
                    boxParams
            );

            windowManager.updateViewLayout(
                    resizeHandle,
                    resizeParams
            );

        } catch (Exception ignored) {
        }
    }

    // ============================================
    // TEXT TO SPEECH
    // ============================================

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            tts.setLanguage(
                    new Locale("id", "ID")
            );
        }
    }

    // ============================================
    // HANCURKAN SERVICE
    // ============================================

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (tts != null) {

            tts.stop();
            tts.shutdown();
        }

        try {

            if (overlayBox != null) {
                windowManager.removeView(
                        overlayBox
                );
            }

            if (resizeHandle != null) {
                windowManager.removeView(
                        resizeHandle
                );
            }

            if (controlLayout != null) {
                windowManager.removeView(
                        controlLayout
                );
            }

        } catch (Exception ignored) {
        }
    }
                                    }
