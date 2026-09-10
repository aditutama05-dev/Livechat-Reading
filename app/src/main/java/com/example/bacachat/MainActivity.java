package com.example.bacachat;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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

        btnStartOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(MainActivity.this)) {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );

                    startActivity(intent);

                } else {
                    startFloatingService();
                }
            }
        });
    }

    private void startFloatingService() {

        boolean isAutoRead = swAutoRead.isChecked();

        Intent serviceIntent =
                new Intent(MainActivity.this, FloatingService.class);

        serviceIntent.putExtra("IS_AUTO_READ", isAutoRead);

        startService(serviceIntent);

        Toast.makeText(
                this,
                "Live Chat Reader aktif",
                Toast.LENGTH_SHORT
        ).show();
    }
}
