package com.example.bacachat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btn_connect);
        if (btnStart != null) {
            btnStart.setText("AKTIFKAN TOMBOL MELAYANG");
            btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkOverlayPermission();
                }
            });
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Izinkan aplikasi tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            } else {
                startFloatingService();
            }
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        startService(new Intent(this, FloatingService.class));
        Toast.makeText(this, "Widget Melayang Aktif!", Toast.LENGTH_SHORT).show();
        finish(); // Otomatis menutup aplikasi utama saat melayang aktif
    }
                    }
            
