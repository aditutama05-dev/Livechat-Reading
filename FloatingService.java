name: Build APK

on:
            cat << 'EOF' >package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View overlayBox;
    private LinearLayout controlLayout;
    private boolean isLocked = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 1. KOTAK HIJAU TRANSPARAN
        overlayBox = new View(this);
        overlayBox.setBackgroundColor(Color.parseColor("#3300FF00"));

        final WindowManager.LayoutParams boxParams = new WindowManager.LayoutParams(
                600, 400,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        boxParams.gravity = Gravity.CENTER;

        // 2. PANEL TOMBOL KONTROL
        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setBackgroundColor(Color.parseColor("#CC000000"));

        final Button btnLock = new Button(this);
        btnLock.setText("🔓 Buka");

        final Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnLock);
        controlLayout.addView(btnClose);

        final WindowManager.LayoutParams controlParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        controlParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        controlParams.y = 100;

        try {
            windowManager.addView(overlayBox, boxParams);
            windowManager.addView(controlLayout, controlParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // FITUR GESER KOTAK
        overlayBox.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isLocked) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = boxParams.x;
                        initialY = boxParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        boxParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        boxParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        try {
                            windowManager.updateViewLayout(overlayBox, boxParams);
                        } catch (Exception ignored) {}
                        return true;
                }
                return false;
            }
        });

        // FITUR TOMBOL KUNCI
        btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLocked = !isLocked;
                if (isLocked) {
                    btnLock.setText("🔒 Terkunci");
                    overlayBox.setBackgroundColor(Color.parseColor("#1100FF00"));
                    Toast.makeText(FloatingService.this, "Kotak Dikunci", Toast.LENGTH_SHORT).show();
                } else {
                    btnLock.setText("🔓 Buka");
                    overlayBox.setBackgroundColor(Color.parseColor("#3300FF00"));
                    Toast.makeText(FloatingService.this, "Kotak Bisa Digeser", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // FITUR TOMBOL TUTUP
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (overlayBox != null) windowManager.removeView(overlayBox);
            if (controlLayout != null) windowManager.removeView(controlLayout);
        } catch (Exception ignored) {}
    }
                                                                   }
