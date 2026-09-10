package com.example.bacachat;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat swAutoRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swAutoRead = findViewById(R.id.swAutoRead);

        Button btnStartOverlay = findViewById(R.id.btnStartOverlay);

        btnStartOverlay.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(this)) {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

            } else {
                startFloatingService();
            }
        });
    }

    private void startFloatingService() {

        boolean autoRead = swAutoRead.isChecked();

        Intent serviceIntent =
                new Intent(this, FloatingService.class);

        serviceIntent.putExtra("IS_AUTO_READ", autoRead);

        startService(serviceIntent);

        Toast.makeText(
                this,
                "Live Chat Reader aktif",
                Toast.LENGTH_SHORT
        ).show();
    }
}
