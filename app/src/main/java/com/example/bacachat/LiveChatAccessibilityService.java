package com.example.bacachat;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LiveChatAccessibilityService extends AccessibilityService {

    private TextToSpeech tts;
    private String lastText = "";
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable pendingRead = new Runnable() {
        @Override
        public void run() {
            AccessibilityNodeInfo root = getRootInActiveWindow();

            if (root == null) {
                return;
            }

            String text = collectVisibleText(root);

            if (text.isEmpty()) {
                return;
            }

            if (!text.equals(lastText)) {
                lastText = text;

                if (tts != null &&
                        tts.getEngines() != null &&
                        !tts.isSpeaking()) {

                    tts.speak(
                            text,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "livechat_read"
                    );
                }
            }

            root.recycle();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
                tts.setSpeechRate(1.0f);
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            handler.removeCallbacks(pendingRead);

            // Debounce supaya perubahan UI yang terjadi beruntun
            // tidak langsung membuat banyak pembacaan.
            handler.postDelayed(pendingRead, 500);
        }
    }

    @Override
    public void onInterrupt() {
        if (tts != null) {
            tts.stop();
        }
    }

    private String collectVisibleText(AccessibilityNodeInfo node) {

        Set<String> uniqueTexts = new HashSet<>();

        collectNodeText(node, uniqueTexts);

        StringBuilder result = new StringBuilder();

        for (String text : uniqueTexts) {

            if (text == null) {
                continue;
            }

            text = text.trim();

            if (text.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(". ");
            }

            result.append(text);
        }

        return result.toString();
    }

    private void collectNodeText(
            AccessibilityNodeInfo node,
            Set<String> result) {

        if (node == null) {
            return;
        }

        CharSequence text = node.getText();

        if (text != null) {
            String value = text.toString().trim();

            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        CharSequence description = node.getContentDescription();

        if (description != null) {
            String value = description.toString().trim();

            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {
                collectNodeText(child, result);
                child.recycle();
            }
        }
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        super.onDestroy();
    }
}
