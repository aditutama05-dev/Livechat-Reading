package com.example.bacachat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etLiveId;
    private Button btnConnect, btnStop;
    private TextView tvChatLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Menghubungkan variabel dengan ID di XML
        etLiveId = findViewById(R.id.et_live_id);
        btnConnect = findViewById(R.id.btn_connect);
        btnStop = findViewById(R.id.btn_stop);
        tvChatLog = findViewById(R.id.tv_chat_log);

        // Fungsi Tombol Mulai Baca
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputId = etLiveId.getText().toString().trim();
                if (inputId.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Masukkan ID/Link terlebih dahulu!", Toast.LENGTH_SHORT).show();
                } else {
                    tvChatLog.setText("Menghubungkan ke " + inputId + "...\nChat akan tampil di sini.");
                    Toast.makeText(MainActivity.this, "Menghubungkan...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Fungsi Tombol Stop
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvChatLog.setText("Status: Pembacaan chat dihentikan.");
                Toast.makeText(MainActivity.this, "Dihentikan", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
