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
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 100;

    private int screenCaptureResultCode;
    private Intent screenCaptureData;

    private final ActivityResultLauncher<Intent> screenCaptureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            screenCaptureResultCode = result.getResultCode();
                            screenCaptureData = result.getData();

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

        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> startReader());
    }

    private void startReader() {

        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                    this,
                    "Izinkan aplikasi tampil di atas aplikasi lain terlebih dahulu.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivityForResult(intent, REQUEST_OVERLAY);

            return;
        }

        requestScreenCapture();
    }

    private void requestScreenCapture() {

        MediaProjectionManager projectionManager =
                (MediaProjectionManager)
                        getSystemService(MEDIA_PROJECTION_SERVICE);

        Intent captureIntent =
                projectionManager.createScreenCaptureIntent();

        screenCaptureLauncher.launch(captureIntent);
    }

    private void startFloatingService() {

        Intent serviceIntent =
                new Intent(this, FloatingService.class);

        serviceIntent.putExtra(
                "SCREEN_CAPTURE_RESULT_CODE",
                screenCaptureResultCode
        );

        serviceIntent.putExtra(
                "SCREEN_CAPTURE_DATA",
                screenCaptureData
        );

        startService(serviceIntent);

        Toast.makeText(
                this,
                "Pembaca live chat aktif.",
                Toast.LENGTH_SHORT
        ).show();
    }
                }
