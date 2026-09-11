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

public class FloatingService extends Service implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;
    private View overlayBox;
    private View resizeHandle;
    private LinearLayout controlLayout;
    private TextToSpeech tts;

    private boolean isLocked = false;
    private boolean isHidden = false;

    private WindowManager.LayoutParams boxParams;

    private int minWidth = 250;
    private int minHeight = 150;

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

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, this);

        int layoutType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // =========================
        // KOTAK AREA LIVE CHAT
        // =========================

        overlayBox = new View(this);
        overlayBox.setBackgroundColor(Color.parseColor("#3300FF88"));

        boxParams = new WindowManager.LayoutParams(
                650,
                450,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        boxParams.gravity = Gravity.CENTER;

        // =========================
        // HANDLE RESIZE
        // =========================

        resizeHandle = new View(this);
        resizeHandle.setBackgroundColor(Color.parseColor("#FF00AA88"));

        WindowManager.LayoutParams resizeParams =
                new WindowManager.LayoutParams(
                        60,
                        60,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        resizeParams.gravity = Gravity.CENTER;
        resizeParams.x = 295;
        resizeParams.y = 195;

        // =========================
        // CONTROL
        // =========================

        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setPadding(10, 5, 10, 5);

        Button btnLock = new Button(this);
        btnLock.setText("🔒 Lock");

        Button btnHide = new Button(this);
        btnHide.setText("👁️ Sembunyi");

        Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

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

        controlParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        controlParams.y = 120;

        try {
            windowManager.addView(overlayBox, boxParams);
            windowManager.addView(resizeHandle, resizeParams);
            windowManager.addView(controlLayout, controlParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // =========================
        // GESER KOTAK
        // =========================

        overlayBox.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (isLocked) {
                    return false;
                }

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialX = boxParams.x;
                        initialY = boxParams.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        boxParams.x =
                                initialX +
                                (int) (event.getRawX() - initialTouchX);

                        boxParams.y =
                                initialY +
                                (int) (event.getRawY() - initialTouchY);

                        updateResizeHandlePosition();

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
        });

        // =========================
        // RESIZE KOTAK
        // =========================

        resizeHandle.setOnTouchListener(new View.OnTouchListener() {

            private int initialWidth;
            private int initialHeight;

            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (isLocked) {
                    return false;
                }

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialWidth = boxParams.width;
                        initialHeight = boxParams.height;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        int newWidth =
                                initialWidth +
                                (int) (event.getRawX() - initialTouchX);

                        int newHeight =
                                initialHeight +
                                (int) (event.getRawY() - initialTouchY);

                        if (newWidth >= minWidth) {
                            boxParams.width = newWidth;
                        }

                        if (newHeight >= minHeight) {
                            boxParams.height = newHeight;
                        }

                        try {
                            windowManager.updateViewLayout(
                                    overlayBox,
                                    boxParams
                            );

                            updateResizeHandlePosition();

                        } catch (Exception ignored) {
                        }

                        return true;
                }

                return false;
            }
        });

        // =========================
        // LOCK
        // =========================

        btnLock.setOnClickListener(v -> {

            isLocked = !isLocked;

            if (isLocked) {

                btnLock.setText("🔒 Terkunci");

                resizeHandle.setVisibility(View.GONE);

                Toast.makeText(
                        FloatingService.this,
                        "Kotak area dikunci",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                btnLock.setText("🔓 Lock");

                resizeHandle.setVisibility(View.VISIBLE);

                Toast.makeText(
                        FloatingService.this,
                        "Kotak bisa digeser dan diubah ukurannya",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // =========================
        // HIDE / SHOW
        // =========================

        btnHide.setOnClickListener(v -> {

            isHidden = !isHidden;

            if (isHidden) {

                overlayBox.setVisibility(View.GONE);
                resizeHandle.setVisibility(View.GONE);

                btnHide.setText("👁️ Tampil");

            } else {

                overlayBox.setVisibility(View.VISIBLE);

                if (!isLocked) {
                    resizeHandle.setVisibility(View.VISIBLE);
                }

                btnHide.setText("👁️ Sembunyi");
            }
        });

        // =========================
        // CLOSE
        // =========================

        btnClose.setOnClickListener(v -> stopSelf());

        updateResizeHandlePosition();
    }

    private void updateResizeHandlePosition() {

        if (boxParams == null || resizeHandle == null) {
            return;
        }

        int handleX =
                boxParams.x +
                boxParams.width -
                30;

        int handleY =
                boxParams.y +
                boxParams.height -
                30;

        WindowManager.LayoutParams params =
                (WindowManager.LayoutParams)
                        resizeHandle.getLayoutParams();

        if (params != null) {

            params.x = handleX;
            params.y = handleY;

            try {
                windowManager.updateViewLayout(
                        resizeHandle,
                        params
                );
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("id", "ID"));
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        try {

            if (overlayBox != null) {
                windowManager.removeView(overlayBox);
            }

            if (resizeHandle != null) {
                windowManager.removeView(resizeHandle);
            }

            if (controlLayout != null) {
                windowManager.removeView(controlLayout);
            }

        } catch (Exception ignored) {
        }
    }
                    }
