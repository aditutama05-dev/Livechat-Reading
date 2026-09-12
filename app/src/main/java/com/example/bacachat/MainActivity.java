package com.example.bacachat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 100;

    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);

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

        startFloatingService();
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingService.class);

        startService(serviceIntent);

        Toast.makeText(
                this,
                "Pembaca live chat aktif.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Settings.canDrawOverlays(this)) {
            btnStart.setText("▶ Mulai Pembaca Live Chat");
        }
    }
}
