package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class FloatingService extends Service {

    private WindowManager windowManager;

    private LinearLayout floatingContainer;
    private TextView mainButton;
    private TextView arrowButton;
    private LinearLayout controlPanel;

    private WindowManager.LayoutParams floatingParams;

    private boolean isLocked = false;
    private boolean controlsVisible = false;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private TextToSpeech textToSpeech;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        setupTextToSpeech();
        createFloatingButton();
    }

    private void setupTextToSpeech() {

        textToSpeech = new TextToSpeech(
                this,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech.setLanguage(
                                new Locale("id", "ID")
                        );
                    }
                }
        );
    }

    private int getOverlayType() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private GradientDrawable createBackground(
            int color,
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    private void createFloatingButton() {

        floatingContainer =
                new LinearLayout(this);

        floatingContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        floatingContainer.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        mainButton =
                new TextView(this);

        mainButton.setText("●");
        mainButton.setTextColor(Color.WHITE);
        mainButton.setTextSize(28);
        mainButton.setGravity(Gravity.CENTER);

        mainButton.setBackground(
                createBackground(
                        Color.rgb(40, 40, 40),
                        100
                )
        );

        arrowButton =
                new TextView(this);

        arrowButton.setText("▼");
        arrowButton.setTextColor(Color.WHITE);
        arrowButton.setTextSize(18);
        arrowButton.setGravity(Gravity.CENTER);

        arrowButton.setBackground(
                createBackground(
                        Color.rgb(55, 55, 55),
                        30
                )
        );

        int buttonSize =
                dpToPx(72);

        LinearLayout.LayoutParams mainParams =
                new LinearLayout.LayoutParams(
                        buttonSize,
                        buttonSize
                );

        LinearLayout.LayoutParams arrowParams =
                new LinearLayout.LayoutParams(
                        dpToPx(72),
                        dpToPx(38)
                );

        arrowParams.topMargin =
                dpToPx(-2);

        floatingContainer.addView(
                mainButton,
                mainParams
        );

        floatingContainer.addView(
                arrowButton,
                arrowParams
        );

        floatingParams =
                new WindowManager.LayoutParams(
                        dpToPx(90),
                        dpToPx(115),
                        getOverlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        floatingParams.gravity =
                Gravity.TOP | Gravity.START;

        floatingParams.x =
                dpToPx(20);

        floatingParams.y =
                dpToPx(200);

        windowManager.addView(
                floatingContainer,
                floatingParams
        );

        mainButton.setOnTouchListener(
                this::handleMainButtonTouch
        );

        arrowButton.setOnClickListener(
                v -> toggleControls()
        );
    }

    private boolean handleMainButtonTouch(
            View view,
            MotionEvent event
    ) {

        if (isLocked) {
            return true;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                initialX =
                        floatingParams.x;

                initialY =
                        floatingParams.y;

                initialTouchX =
                        event.getRawX();

                initialTouchY =
                        event.getRawY();

                return true;

            case MotionEvent.ACTION_MOVE:

                int newX =
                        initialX +
                        (int) (
                                event.getRawX()
                                        - initialTouchX
                        );

                int newY =
                        initialY +
                        (int) (
                                event.getRawY()
                                        - initialTouchY
                        );

                floatingParams.x =
                        newX;

                floatingParams.y =
                        newY;

                windowManager.updateViewLayout(
                        floatingContainer,
                        floatingParams
                );

                return true;

            case MotionEvent.ACTION_UP:

                return true;
        }

        return false;
    }

    private void toggleControls() {

        controlsVisible =
                !controlsVisible;

        if (controlsVisible) {
            showControlPanel();
        } else {
            hideControlPanel();
        }
    }

    private void showControlPanel() {

        if (controlPanel != null) {
            return;
        }

        controlPanel =
                new LinearLayout(this);

        controlPanel.setOrientation(
                LinearLayout.HORIZONTAL
        );

        controlPanel.setGravity(
                Gravity.CENTER
        );

        TextView lockButton =
                createControlButton(
                        isLocked ? "Buka" : "Kunci"
                );

        TextView closeButton =
                createControlButton(
                        "Tutup"
                );

        controlPanel.addView(lockButton);

        controlPanel.addView(closeButton);

        WindowManager.LayoutParams panelParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        dpToPx(55),
                        getOverlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        panelParams.gravity =
                Gravity.TOP | Gravity.START;

        panelParams.x =
                floatingParams.x;

        panelParams.y =
                floatingParams.y
                        + dpToPx(120);

        windowManager.addView(
                controlPanel,
                panelParams
        );

        lockButton.setOnClickListener(v -> {

            isLocked =
                    !isLocked;

            lockButton.setText(
                    isLocked ? "Buka" : "Kunci"
            );
        });

        closeButton.setOnClickListener(v -> {

            stopSelf();
        });
    }

    private TextView createControlButton(
            String text
    ) {

        TextView button =
                new TextView(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);

        button.setPadding(
                dpToPx(18),
                0,
                dpToPx(18),
                0
        );

        button.setBackground(
                createBackground(
                        Color.rgb(50, 50, 50),
                        20
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        dpToPx(50)
                );

        params.setMargins(
                dpToPx(4),
                dpToPx(2),
                dpToPx(4),
                dpToPx(2)
        );

        button.setLayoutParams(params);

        return button;
    }

    private void hideControlPanel() {

        if (controlPanel != null) {

            windowManager.removeView(
                    controlPanel
            );

            controlPanel =
                    null;
        }
    }

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                dp * density
        );
    }

    @Override
    public void onDestroy() {

        hideControlPanel();

        if (floatingContainer != null) {

            windowManager.removeView(
                    floatingContainer
            );

            floatingContainer =
                    null;
        }

        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
            }
