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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Locale;

public class FloatingService extends Service
        implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;

    private FrameLayout overlayBox;
    private View resizeHandle;
    private LinearLayout controlLayout;

    private WindowManager.LayoutParams boxParams;
    private WindowManager.LayoutParams controlParams;
    private WindowManager.LayoutParams resizeParams;

    private TextToSpeech tts;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isReading = false;

    private int minWidth = 250;
    private int minHeight = 180;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, this);

        createFloatingBox();
        createControlPanel();
    }

    private int getLayoutType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createFloatingBox() {

        int layoutType = getLayoutType();

        overlayBox = new FrameLayout(this);
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

        View borderView = new View(this);
        borderView.setBackgroundColor(
                Color.TRANSPARENT
        );

        FrameLayout.LayoutParams borderParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        overlayBox.addView(borderView, borderParams);

        resizeHandle = new View(this);
        resizeHandle.setBackgroundColor(
                Color.parseColor("#FFFFFFFF")
        );

        resizeParams = new WindowManager.LayoutParams(
                45,
                45,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        resizeParams.gravity = Gravity.TOP | Gravity.LEFT;

        try {
            windowManager.addView(
                    overlayBox,
                    boxParams
            );

            windowManager.addView(
                    resizeHandle,
                    resizeParams
            );

            updateResizeHandlePosition();

        } catch (Exception e) {
            e.printStackTrace();
        }

        setupDrag();

        setupResize();
    }

    private void createControlPanel() {

        int layoutType = getLayoutType();

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

        final Button btnRead = new Button(this);
        btnRead.setText("▶️ Baca");

        final Button btnLock = new Button(this);
        btnLock.setText("🔓 Lock");

        final Button btnHide = new Button(this);
        btnHide.setText("👁️ Sembunyi");

        final Button btnClose = new Button(this);
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

        controlParams.y = 100;

        try {
            windowManager.addView(
                    controlLayout,
                    controlParams
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnRead.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        isReading = !isReading;

                        if (isReading) {

                            btnRead.setText("⏸️ Stop");

                            speak(
                                    "Pembacaan diaktifkan."
                            );

                        } else {

                            btnRead.setText("▶️ Baca");

                            speak(
                                    "Pembacaan dihentikan."
                            );
                        }
                    }
                }
        );

        btnLock.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        isLocked = !isLocked;

                        if (isLocked) {

                            btnLock.setText("🔒 Terkunci");

                            Toast.makeText(
                                    FloatingService.this,
                                    "Kotak terkunci",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            btnLock.setText("🔓 Lock");

                            Toast.makeText(
                                    FloatingService.this,
                                    "Kotak bisa digeser dan diubah ukurannya",
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

                        isHidden = !isHidden;

                        if (isHidden) {

                            overlayBox.setVisibility(
                                    View.GONE
                            );

                            resizeHandle.setVisibility(
                                    View.GONE
                            );

                            btnHide.setText(
                                    "👁️ Tampil"
                            );

                        } else {

                            overlayBox.setVisibility(
                                    View.VISIBLE
                            );

                            resizeHandle.setVisibility(
                                    View.VISIBLE
                            );

                            btnHide.setText(
                                    "👁️ Sembunyi"
                            );

                            updateResizeHandlePosition();
                        }
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

                                    updateResizeHandlePosition();

                                } catch (Exception ignored) {
                                }

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

                    private int startWidth;
                    private int startHeight;

                    private float startTouchX;
                    private float startTouchY;

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

                                startWidth =
                                        boxParams.width;

                                startHeight =
                                        boxParams.height;

                                startTouchX =
                                        event.getRawX();

                                startTouchY =
                                        event.getRawY();

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                int newWidth =
                                        startWidth
                                                + (int) (
                                                event.getRawX()
                                                        - startTouchX
                                        );

                                int newHeight =
                                        startHeight
                                                + (int) (
                                                event.getRawY()
                                                        - startTouchY
                                        );

                                if (newWidth < minWidth) {
                                    newWidth = minWidth;
                                }

                                if (newHeight < minHeight) {
                                    newHeight = minHeight;
                                }

                                boxParams.width =
                                        newWidth;

                                boxParams.height =
                                        newHeight;

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
                }
        );
    }

    private void updateResizeHandlePosition() {

        if (resizeHandle == null
                || resizeParams == null
                || boxParams == null) {
            return;
        }

        int left =
                boxParams.x
                        - (boxParams.width / 2);

        int top =
                boxParams.y
                        - (boxParams.height / 2);

        resizeParams.gravity =
                Gravity.TOP | Gravity.LEFT;

        resizeParams.x =
                left + boxParams.width - 45;

        resizeParams.y =
                top + boxParams.height - 45;

        try {

            windowManager.updateViewLayout(
                    resizeHandle,
                    resizeParams
            );

        } catch (Exception ignored) {
        }
    }

    private void speak(String text) {

        if (tts == null) {
            return;
        }

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "BACA_LIVECHAT"
        );
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

        super.onDestroy();

        if (tts != null) {

            tts.stop();
            tts.shutdown();

            tts = null;
        }

        try {

            if (overlayBox != null) {
                windowManager.removeView(
                        overlayBox
                );
            }

        } catch (Exception ignored) {
        }

        try {

            if (resizeHandle != null) {
                windowManager.removeView(
                        resizeHandle
                );
            }

        } catch (Exception ignored) {
        }

        try {

            if (controlLayout != null) {
                windowManager.removeView(
                        controlLayout
                );
            }

        } catch (Exception ignored) {
        }
    }
                }
