package com.example.bacachat;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class LiveChatAccessibilityService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Pembacaan teks layar akan ditambahkan pada tahap berikutnya.
    }

    @Override
    public void onInterrupt() {
        // Dipanggil ketika Accessibility Service dihentikan atau terganggu.
    }
}
