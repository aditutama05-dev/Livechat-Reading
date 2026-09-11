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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button btnStartOverlay =
                findViewById(R.id.btnStartOverlay);

        btnStartOverlay.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                && !Settings.canDrawOverlays(MainActivity.this)) {

                            Intent intent =
                                    new Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse(
                                                    "package:" +
                                                    getPackageName()
                                            )
                                    );

                            startActivityForResult(intent, 100);

                        } else {

                            startFloatingService();
                        }
                    }
                }
        );
    }

    private void startFloatingService() {

        Intent serviceIntent =
                new Intent(
                        MainActivity.this,
                        FloatingService.class
                );

        startService(serviceIntent);

        Toast.makeText(
                this,
                "Ikon melayang aktif!",
                Toast.LENGTH_SHORT
        ).show();
    }
}
