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
    private LinearLayout controlLayout;
    private TextToSpeech tts;
    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isLiveMode = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isLiveMode = intent.getBooleanExtra("IS_LIVE_MODE", true);
            if (!isLiveMode && overlayBox != null) {
                overlayBox.setVisibility(View.GONE);
            } else if (isLiveMode && overlayBox != null && !isHidden) {
                overlayBox.setVisibility(View.VISIBLE);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        tts = new TextToSpeech(this, this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        overlayBox = new View(this);
        overlayBox.setBackgroundColor(Color.parseColor("#3300FF88"));

        final WindowManager.LayoutParams boxParams = new WindowManager.LayoutParams(
                650, 450,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        boxParams.gravity = Gravity.CENTER;

        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setPadding(20, 10, 20, 10);

        final Button btnLock = new Button(this);
        btnLock.setText("🔒 Lock");

        final Button btnHide = new Button(this);
        btnHide.setText("👁️ Sembunyi");

        final Button btnClose = new Button(this);
        btnClose.setText("❌ Tutup");

        controlLayout.addView(btnLock);
        controlLayout.addView(btnHide);
        controlLayout.addView(btnClose);

        final WindowManager.LayoutParams controlParams = new WindowManager.LayoutParams(
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
            windowManager.addView(controlLayout, controlParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLocked = !isLocked;
                btnLock.setText(isLocked ? "🔒 Terkunci" : "🔓 Lock");
                Toast.makeText(FloatingService.this, isLocked ? "Kotak Terkunci" : "Kotak Bebas Digeser", Toast.LENGTH_SHORT).show();
            }
        });

        btnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHidden = !isHidden;
                if (isLiveMode) {
                    overlayBox.setVisibility(isHidden ? View.GONE : View.VISIBLE);
                }
                btnHide.setText(isHidden ? "👁️ Tampil" : "👁️ Sembunyi");
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
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
            if (overlayBox != null) windowManager.removeView(overlayBox);
            if (controlLayout != null) windowManager.removeView(controlLayout);
        } catch (Exception ignored) {}
    }
    }
          
