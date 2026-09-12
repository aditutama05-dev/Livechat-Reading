package com.example.bacachat;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 100;

    private int screenCaptureResultCode;
    private Intent screenCaptureData;

    private LanguageSettings languageSettings;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            screenCaptureResultCode =
                                    result.getResultCode();

                            screenCaptureData =
                                    result.getData();

                            startFloatingService();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Izin menangkap layar diperlukan.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        languageSettings =
                new LanguageSettings(this);

        Button btnStart =
                findViewById(R.id.btnStart);

        Button btnLanguageSettings =
                findViewById(R.id.btnLanguageSettings);

        btnStart.setOnClickListener(
                v -> startReader()
        );

        btnLanguageSettings.setOnClickListener(
                v -> showLanguageSettings()
        );
    }

    private void startReader() {

        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                    this,
                    "Izinkan aplikasi tampil di atas aplikasi lain terlebih dahulu.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent =
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse(
                                    "package:" + getPackageName()
                            )
                    );

            startActivityForResult(
                    intent,
                    REQUEST_OVERLAY
            );

            return;
        }

        requestScreenCapture();
    }

    private void requestScreenCapture() {

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        if (projectionManager == null) {

            Toast.makeText(
                    this,
                    "MediaProjection tidak tersedia.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Intent captureIntent =
                projectionManager.createScreenCaptureIntent();

        screenCaptureLauncher.launch(
                captureIntent
        );
    }

    private void startFloatingService() {

        Intent serviceIntent =
                new Intent(
                        this,
                        FloatingService.class
                );

        serviceIntent.putExtra(
                "SCREEN_CAPTURE_RESULT_CODE",
                screenCaptureResultCode
        );

        serviceIntent.putExtra(
                "SCREEN_CAPTURE_DATA",
                screenCaptureData
        );

        startService(
                serviceIntent
        );

        Toast.makeText(
                this,
                "Pembaca live chat aktif.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showLanguageSettings() {

        String[] options = {
                "🔊 Bahasa TTS",
                "🔤 Bahasa OCR"
        };

        new AlertDialog.Builder(this)
                .setTitle("🌐 Pengaturan Bahasa")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {
                                showTtsLanguageDialog();
                            } else {
                                showOcrLanguageDialog();
                            }
                        }
                )
                .show();
    }

    private void showTtsLanguageDialog() {

        String[] languages = {
                "English",
                "Indonesia",
                "日本語",
                "한국어",
                "中文",
                "Español",
                "Français",
                "Deutsch"
        };

        String[] codes = {
                "en-US",
                "id-ID",
                "ja-JP",
                "ko-KR",
                "zh-CN",
                "es-ES",
                "fr-FR",
                "de-DE"
        };

        int selected = findSelectedLanguage(
                languageSettings.getTtsLanguage(),
                codes
        );

        new AlertDialog.Builder(this)
                .setTitle("🔊 Bahasa TTS")
                .setSingleChoiceItems(
                        languages,
                        selected,
                        (dialog, which) -> {

                            languageSettings.setTtsLanguage(
                                    codes[which]
                            );

                            Toast.makeText(
                                    this,
                                    "Bahasa TTS: "
                                            + languages[which],
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        }
                )
                .show();
    }

    private void showOcrLanguageDialog() {

        String[] languages = {
                "English",
                "Indonesia",
                "日本語",
                "한국어",
                "中文",
                "Español",
                "Français",
                "Deutsch"
        };

        String[] codes = {
                "en",
                "id",
                "ja",
                "ko",
                "zh",
                "es",
                "fr",
                "de"
        };

        int selected = findSelectedLanguage(
                languageSettings.getOcrLanguage(),
                codes
        );

        new AlertDialog.Builder(this)
                .setTitle("🔤 Bahasa OCR")
                .setSingleChoiceItems(
                        languages,
                        selected,
                        (dialog, which) -> {

                            languageSettings.setOcrLanguage(
                                    codes[which]
                            );

                            Toast.makeText(
                                    this,
                                    "Bahasa OCR: "
                                            + languages[which],
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        }
                )
                .show();
    }

    private int findSelectedLanguage(
            String current,
            String[] codes) {

        for (int i = 0; i < codes.length; i++) {

            if (codes[i].equals(current)) {
                return i;
            }
        }

        return 0;
    }
    }
