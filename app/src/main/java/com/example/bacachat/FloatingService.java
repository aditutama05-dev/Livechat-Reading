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
import android.util.DisplayMetrics;
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
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.Locale;

public class FloatingService extends Service implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;
    private View overlayBox;
    private LinearLayout controlLayout;

    private TextToSpeech textToSpeech;

    private boolean isLocked = false;
    private boolean isHidden = false;
    private boolean isCapturing = false;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;

    private HandlerThread captureThread;
    private Handler captureHandler;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private WindowManager.LayoutParams boxParams;

    private TextRecognizer latinRecognizer;
    private TextRecognizer chineseRecognizer;
    private TextRecognizer devanagariRecognizer;
    private TextRecognizer japaneseRecognizer;
    private TextRecognizer koreanRecognizer;

    private LanguageIdentifier languageIdentifier;

    private String lastSpokenText = "";

    private float downX;
    private float downY;
    private int startX;
    private int startY;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        textToSpeech = new TextToSpeech(this, this);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        latinRecognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );

        chineseRecognizer = TextRecognition.getClient(
                new ChineseTextRecognizerOptions.Builder().build()
        );

        devanagariRecognizer = TextRecognition.getClient(
                new DevanagariTextRecognizerOptions.Builder().build()
        );

        japaneseRecognizer = TextRecognition.getClient(
                new JapaneseTextRecognizerOptions.Builder().build()
        );

        koreanRecognizer = TextRecognition.getClient(
                new KoreanTextRecognizerOptions.Builder().build()
        );

        languageIdentifier = LanguageIdentification.getClient();

        captureThread = new HandlerThread("LiveChatCapture");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());

        createOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            int resultCode = intent.getIntExtra(
                    "SCREEN_CAPTURE_RESULT_CODE",
                    -1
            );

            Intent resultData = intent.getParcelableExtra(
                    "SCREEN_CAPTURE_DATA"
            );

            if (resultCode != -1 && resultData != null) {
                setupMediaProjection(resultCode, resultData);
            }
        }

        return START_STICKY;
    }

    private void createOverlay() {

        int overlayType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        boxParams = new WindowManager.LayoutParams(
                650,
                450,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        boxParams.gravity = Gravity.TOP | Gravity.START;
        boxParams.x = 100;
        boxParams.y = 250;

        overlayBox = new View(this);
        overlayBox.setBackgroundColor(
                Color.argb(60, 0, 255, 0)
        );

        windowManager.addView(overlayBox, boxParams);

        createControlButtons(overlayType);
    }

    private void createControlButtons(int overlayType) {

        controlLayout = new LinearLayout(this);
        controlLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlLayout.setPadding(8, 8, 8, 8);
        controlLayout.setBackgroundColor(
                Color.argb(210, 20, 20, 20)
        );

        Button lockButton = new Button(this);
        lockButton.setText("Kunci");

        Button hideButton = new Button(this);
        hideButton.setText("Sembunyikan");

        Button readButton = new Button(this);
        readButton.setText("Baca");

        Button closeButton = new Button(this);
        closeButton.setText("Tutup");

        controlLayout.addView(lockButton);
        controlLayout.addView(hideButton);
        controlLayout.addView(readButton);
        controlLayout.addView(closeButton);

        WindowManager.LayoutParams controlParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                );

        controlParams.gravity = Gravity.TOP | Gravity.START;
        controlParams.x = 100;
        controlParams.y = 180;

        windowManager.addView(controlLayout, controlParams);

        lockButton.setOnClickListener(v -> {

            isLocked = !isLocked;

            if (isLocked) {
                lockButton.setText("Buka");
                Toast.makeText(
                        this,
                        "Area live chat dikunci",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                lockButton.setText("Kunci");
                Toast.makeText(
                        this,
                        "Area live chat dapat digeser",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        hideButton.setOnClickListener(v -> {

            isHidden = !isHidden;

            if (isHidden) {
                overlayBox.setVisibility(View.GONE);
                controlLayout.setVisibility(View.GONE);
            } else {
                overlayBox.setVisibility(View.VISIBLE);
                controlLayout.setVisibility(View.VISIBLE);
            }
        });

        readButton.setOnClickListener(v -> {

            if (!isCapturing) {
                Toast.makeText(
                        this,
                        "Screen capture belum siap",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            captureCurrentFrame(true);
        });

        closeButton.setOnClickListener(v -> stopSelf());

        overlayBox.setOnTouchListener((v, event) -> {

            if (isLocked) {
                return true;
            }

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    downX = event.getRawX();
                    downY = event.getRawY();

                    startX = boxParams.x;
                    startY = boxParams.y;

                    return true;

                case MotionEvent.ACTION_MOVE:

                    float moveX = event.getRawX() - downX;
                    float moveY = event.getRawY() - downY;

                    boxParams.x = startX + (int) moveX;
                    boxParams.y = startY + (int) moveY;

                    windowManager.updateViewLayout(
                            overlayBox,
                            boxParams
                    );

                    return true;

                case MotionEvent.ACTION_UP:

                    return true;
            }

            return false;
        });
    }

    private void setupMediaProjection(
            int resultCode,
            Intent resultData
    ) {

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        if (projectionManager == null) {
            return;
        }

        mediaProjection =
                projectionManager.getMediaProjection(
                        resultCode,
                        resultData
                );

        if (mediaProjection != null) {
            startScreenCapture();
        }
    }

    private void startScreenCapture() {

        if (mediaProjection == null) {
            return;
        }

        if (imageReader != null) {
            return;
        }

        imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(
                reader -> {

                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();

                        if (image == null) {
                            return;
                        }

                        processFrame(image, false);

                    } catch (Exception e) {

                        e.printStackTrace();

                    } finally {

                        if (image != null) {
                            image.close();
                        }
                    }
                },
                captureHandler
        );

        mediaProjection.createVirtualDisplay(
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
    }

    private void captureCurrentFrame(boolean runOcr) {

        if (imageReader == null) {
            return;
        }

        captureHandler.post(() -> {

            Image image = null;

            try {

                image = imageReader.acquireLatestImage();

                if (image == null) {
                    return;
                }

                processFrame(image, runOcr);

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                if (image != null) {
                    image.close();
                }
            }
        });
    }

    private void processFrame(
            Image image,
            boolean runOcr
    ) {

        Image.Plane[] planes = image.getPlanes();

        if (planes.length == 0) {
            return;
        }

        ByteBuffer buffer = planes[0].getBuffer();

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();

        int rowPadding =
                rowStride - pixelStride * screenWidth;

        int bitmapWidth =
                screenWidth + rowPadding / pixelStride;

        Bitmap fullBitmap = null;
        Bitmap croppedBitmap = null;

        try {

            fullBitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
            );

            buffer.rewind();

            fullBitmap.copyPixelsFromBuffer(buffer);

            int cropLeft = Math.max(
                    0,
                    boxParams.x
            );

            int cropTop = Math.max(
                    0,
                    boxParams.y
            );

            int cropRight = Math.min(
                    screenWidth,
                    boxParams.x + boxParams.width
            );

            int cropBottom = Math.min(
                    screenHeight,
                    boxParams.y + boxParams.height
            );

            int cropWidth = cropRight - cropLeft;
            int cropHeight = cropBottom - cropTop;

            if (cropWidth <= 0 || cropHeight <= 0) {
                return;
            }

            croppedBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    cropLeft,
                    cropTop,
                    cropWidth,
                    cropHeight
            );

            if (runOcr) {
                runOcr(croppedBitmap);
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (croppedBitmap != null) {
                croppedBitmap.recycle();
            }

            if (fullBitmap != null) {
                fullBitmap.recycle();
            }
        }
    }

    private void runOcr(Bitmap bitmap) {

        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }

        InputImage inputImage =
                InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer =
                chooseRecognizer(bitmap);

        recognizer.process(inputImage)
                .addOnSuccessListener(result -> {

                    String detectedText =
                            result.getText();

                    if (detectedText == null) {
                        return;
                    }

                    detectedText =
                            detectedText.trim();

                    if (detectedText.isEmpty()) {
                        return;
                    }

                    identifyLanguageAndSpeak(
                            detectedText
                    );
                })
                .addOnFailureListener(
                        Throwable::printStackTrace
                );
    }

    private TextRecognizer chooseRecognizer(
            Bitmap bitmap
    ) {

        /*
         * Untuk tahap awal kita menggunakan
         * Latin recognizer sebagai recognizer utama.
         *
         * Recognizer untuk Chinese, Japanese,
         * Korean, dan Devanagari sudah disiapkan
         * untuk tahap deteksi script berikutnya.
         */

        return latinRecognizer;
    }

    private void identifyLanguageAndSpeak(
            String text
    ) {

        if (languageIdentifier == null) {
            speakText(text, Locale.getDefault());
            return;
        }

        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {

                    Locale detectedLocale =
                            localeFromLanguageCode(
                                    languageCode
                            );

                    speakText(
                            text,
                            detectedLocale
                    );
                })
                .addOnFailureListener(e -> {

                    speakText(
                            text,
                            Locale.getDefault()
                    );
                });
    }

    private Locale localeFromLanguageCode(
            String languageCode
    ) {

        if (languageCode == null ||
                languageCode.isEmpty() ||
                languageCode.equals("und")) {

            return Locale.getDefault();
        }

        String normalized =
                languageCode.replace(
                        '_',
                        '-'
                );

        String[] parts =
                normalized.split("-");

        if (parts.length >= 2) {

            return new Locale(
                    parts[0],
                    parts[1]
            );
        }

        return new Locale(parts[0]);
    }

    private void speakText(
            String text,
            Locale language
    ) {

        if (textToSpeech == null) {
            return;
        }

        if (text.equals(lastSpokenText)) {
            return;
        }

        lastSpokenText = text;

        int languageResult =
                textToSpeech.setLanguage(language);

        if (languageResult ==
                TextToSpeech.LANG_MISSING_DATA ||
                languageResult ==
                        TextToSpeech.LANG_NOT_SUPPORTED) {

            Locale deviceLocale =
                    Locale.getDefault();

            textToSpeech.setLanguage(
                    deviceLocale
            );
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "livechat_text"
        );
    }

    private void stopScreenCapture() {

        isCapturing = false;

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
    public void onInit(int status) {

        if (status ==
                TextToSpeech.SUCCESS) {

            Locale deviceLocale =
                    Locale.getDefault();

            int result =
                    textToSpeech.setLanguage(
                            deviceLocale
                    );

            if (result ==
                    TextToSpeech.LANG_MISSING_DATA ||
                    result ==
                            TextToSpeech.LANG_NOT_SUPPORTED) {

                textToSpeech.setLanguage(
                        Locale.US
                );
            }
        }
    }

    @Override
    public void onDestroy() {

        stopScreenCapture();

        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        if (latinRecognizer != null) {
            latinRecognizer.close();
            latinRecognizer = null;
        }

        if (chineseRecognizer != null) {
            chineseRecognizer.close();
            chineseRecognizer = null;
        }

        if (devanagariRecognizer != null) {
            devanagariRecognizer.close();
            devanagariRecognizer = null;
        }

        if (japaneseRecognizer != null) {
            japaneseRecognizer.close();
            japaneseRecognizer = null;
        }

        if (koreanRecognizer != null) {
            koreanRecognizer.close();
            koreanRecognizer = null;
        }

        if (languageIdentifier != null) {
            languageIdentifier.close();
            languageIdentifier = null;
        }

        if (overlayBox != null) {

            try {
                windowManager.removeView(
       
