package com.example.bacachat;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {

    private WindowManager windowManager;

    private LinearLayout floatingContainer;
    private TextView mainButton;
    private TextView arrowButton;
    private LinearLayout controlPanel;

    private FrameLayout regionContainer;
    private View regionBox;
    private View resizeHandle;

    private WindowManager.LayoutParams floatingParams;
    private WindowManager.LayoutParams regionParams;

    private boolean floatingLocked = false;
    private boolean regionLocked = false;
    private boolean controlsVisible = false;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private int initialRegionX;
    private int initialRegionY;
    private float initialRegionTouchX;
    private float initialRegionTouchY;

    private int initialRegionWidth;
    private int initialRegionHeight;
    private float initialResizeTouchX;
    private float initialResizeTouchY;

    private final int MIN_REGION_WIDTH_DP = 120;
    private final int MIN_REGION_HEIGHT_DP = 80;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        createRegionBox();
        createFloatingButton();
    }

    private int getOverlayType() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (dp * density);
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

    private void createRegionBox() {

        regionContainer =
                new FrameLayout(this);

        regionBox =
                new View(this);

        GradientDrawable border =
                new GradientDrawable();

        border.setColor(
                Color.argb(35, 255, 255, 255)
        );

        border.setStroke(
                dpToPx(3),
                Color.WHITE
        );

        regionBox.setBackground(border);

        regionContainer.addView(
                regionBox,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        resizeHandle =
                new View(this);

        GradientDrawable handleBackground =
                createBackground(
                        Color.WHITE,
                        dpToPx(20)
                );

        resizeHandle.setBackground(
                handleBackground
        );

        FrameLayout.LayoutParams handleParams =
                new FrameLayout.LayoutParams(
                        dpToPx(30),
                        dpToPx(30)
                );

        handleParams.gravity =
                Gravity.BOTTOM | Gravity.END;

        handleParams.setMargins(
                0,
                0,
                dpToPx(5),
                dpToPx(5)
        );

        regionContainer.addView(
                resizeHandle,
                handleParams
        );

        regionParams =
                new WindowManager.LayoutParams(
                        dpToPx(300),
                        dpToPx(180),
                        getOverlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        regionParams.gravity =
                Gravity.TOP | Gravity.START;

        regionParams.x =
                dpToPx(40);

        regionParams.y =
                dpToPx(300);

        windowManager.addView(
                regionContainer,
                regionParams
        );

        regionBox.setOnTouchListener(
                this::handleRegionMove
        );

        resizeHandle.setOnTouchListener(
                this::handleRegionResize
        );
    }

    private boolean handleRegionMove(
            View view,
            MotionEvent event
    ) {

        if (regionLocked) {
            return true;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                initialRegionX =
                        regionParams.x;

                initialRegionY =
                        regionParams.y;

                initialRegionTouchX =
                        event.getRawX();

                initialRegionTouchY =
                        event.getRawY();

                return true;

            case MotionEvent.ACTION_MOVE:

                regionParams.x =
                        initialRegionX +
                        (int) (
                                event.getRawX()
                                        - initialRegionTouchX
                        );

                regionParams.y =
                        initialRegionY +
                        (int) (
                                event.getRawY()
                                        - initialRegionTouchY
                        );

                windowManager.updateViewLayout(
                        regionContainer,
                        regionParams
                );

                return true;

            case MotionEvent.ACTION_UP:

                return true;
        }

        return false;
    }

    private boolean handleRegionResize(
            View view,
            MotionEvent event
    ) {

        if (regionLocked) {
            return true;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                initialRegionWidth =
                        regionParams.width;

                initialRegionHeight =
                        regionParams.height;

                initialResizeTouchX =
                        event.getRawX();

                initialResizeTouchY =
                        event.getRawY();

                return true;

            case MotionEvent.ACTION_MOVE:

                int newWidth =
                        initialRegionWidth +
                        (int) (
                                event.getRawX()
                                        - initialResizeTouchX
                        );

                int newHeight =
                        initialRegionHeight +
                        (int) (
                                event.getRawY()
                                        - initialResizeTouchY
                        );

                newWidth =
                        Math.max(
                                newWidth,
                                dpToPx(
                                        MIN_REGION_WIDTH_DP
                                )
                        );

                newHeight =
                        Math.max(
                                newHeight,
                                dpToPx(
                                        MIN_REGION_HEIGHT_DP
                                )
                        );

                regionParams.width =
                        newWidth;

                regionParams.height =
                        newHeight;

                windowManager.updateViewLayout(
                        regionContainer,
                        regionParams
                );

                return true;

            case MotionEvent.ACTION_UP:

                return true;
        }

        return false;
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
                dpToPx(150);

        windowManager.addView(
                floatingContainer,
                floatingParams
        );

        mainButton.setOnTouchListener(
                this::handleFloatingTouch
        );

        arrowButton.setOnClickListener(
                v -> toggleControls()
        );
    }

    private boolean handleFloatingTouch(
            View view,
            MotionEvent event
    ) {

        if (floatingLocked) {
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

                floatingParams.x =
                        initialX +
                        (int) (
                                event.getRawX()
                                        - initialTouchX
                        );

                floatingParams.y =
                        initialY +
                        (int) (
                                event.getRawY()
                                        - initialTouchY
                        );

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
                LinearLayout.VERTICAL
        );

        controlPanel.setGravity(
                Gravity.CENTER
        );

        TextView lockRegion =
                createControlButton(
                        regionLocked
                                ? "Buka Area"
                                : "Kunci Area"
                );

        TextView lockFloating =
                createControlButton(
                        floatingLocked
                                ? "Buka Tombol"
                                : "Kunci Tombol"
                );

        TextView closeButton =
                createControlButton(
                        "Tutup"
                );

        controlPanel.addView(lockRegion);
        controlPanel.addView(lockFloating);
        controlPanel.addView(closeButton);

        WindowManager.LayoutParams panelParams =
                new WindowManager.LayoutParams(
                        dpToPx(150),
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        getOverlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        panelParams.gravity =
                Gravity.TOP | Gravity.START;

        panelParams.x =
                floatingParams.x;

        panelParams.y =
                floatingParams.y +
                        dpToPx(120);

        windowManager.addView(
                controlPanel,
                panelParams
        );

        lockRegion.setOnClickListener(v -> {

            regionLocked =
                    !regionLocked;

            lockRegion.setText(
                    regionLocked
                            ? "Buka Area"
                            : "Kunci Area"
            );
        });

        lockFloating.setOnClickListener(v -> {

            floatingLocked =
                    !floatingLocked;

            lockFloating.setText(
                    floatingLocked
                            ? "Buka Tombol"
                            : "Kunci Tombol"
            );
        });

        closeButton.setOnClickListener(
                v -> stopSelf()
        );
    }

    private TextView createControlButton(
            String text
    ) {

        TextView button =
                new TextView(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);

        button.setPadding(
                dpToPx(10),
                dpToPx(10),
                dpToPx(10),
                dpToPx(10)
        );

        button.setBackground(
                createBackground(
                        Color.rgb(50, 50, 50),
                        15
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dpToPx(140),
                        dpToPx(50)
                );

        params.setMargins(
                dpToPx(3),
                dpToPx(3),
                dpToPx(3),
                dpToPx(3)
        );

        button.setLayoutParams(params);

        return button;
    }

    private void hideControlPanel() {

        if (controlPanel != null) {

            windowManager.removeView(
                    controlPanel
            );

            controlPanel = null;
        }
    }

    @Override
    public void onDestroy() {

        hideControlPanel();

        if (regionContainer != null) {

            windowManager.removeView(
                    regionContainer
            );

            regionContainer = null;
        }

        if (floatingContainer != null) {

            windowManager.removeView(
                    floatingContainer
            );

            floatingContainer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
