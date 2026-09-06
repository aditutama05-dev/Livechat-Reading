package com.example.bacachat;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private RadioButton rbLiveStream;
    private Switch swAutoRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rbLiveStream = findViewById(R.id.rbLiveStream);
        swAutoRead = findViewById(R.id.swAutoRead);
        Button btnStartOverlay = findViewById(R.id.btnStartOverlay);

        btnStartOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 100);
                } else {
                    startFloatingService();
                }
            }
        });
    }

    private void startFloatingService() {
        boolean isLiveMode = rbLiveStream.isChecked();
        boolean isAutoRead = swAutoRead.isChecked();

        Intent serviceIntent = new Intent(MainActivity.this, FloatingService.class);
        serviceIntent.putExtra("IS_LIVE_MODE", isLiveMode);
        serviceIntent.putExtra("IS_AUTO_READ", isAutoRead);
        startService(serviceIntent);

        Toast.makeText(this, "Ikon Melayang Aktif!", Toast.LENGTH_SHORT).show();
    }
                                    }
      
