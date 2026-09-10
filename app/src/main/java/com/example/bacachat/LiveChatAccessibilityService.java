package com.example.bacachat;

import android.accessibilityservice.AccessibilityService;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LiveChatAccessibilityService extends AccessibilityService {

    private TextToSpeech textToSpeech;

    private String lastText = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        textToSpeech =
                new TextToSpeech(
                        this,
                        status -> {

                            if (status == TextToSpeech.SUCCESS) {

                                textToSpeech.setLanguage(
                                        new Locale("id", "ID")
                                );
                            }
                        }
                );
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event.getEventType()
                != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && event.getEventType()
                != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {

            return;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {
            return;
        }

        List<String> texts =
                new ArrayList<>();

        collectText(
                root,
                texts
        );

        root.recycle();

        if (texts.isEmpty()) {
            return;
        }

        StringBuilder result =
                new StringBuilder();

        for (String text : texts) {

            if (result.length() > 0) {
                result.append(". ");
            }

            result.append(text);
        }

        String currentText =
                result.toString().trim();

        if (currentText.isEmpty()) {
            return;
        }

        if (currentText.equals(lastText)) {
            return;
        }

        lastText = currentText;

        speak(currentText);
    }

    private void collectText(
            AccessibilityNodeInfo node,
            List<String> texts
    ) {

        if (node == null) {
            return;
        }

        CharSequence text =
                node.getText();

        if (text != null) {

            String value =
                    text.toString().trim();

            if (!value.isEmpty()
                    && !texts.contains(value)) {

                texts.add(value);
            }
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null) {

            String value =
                    description.toString().trim();

            if (!value.isEmpty()
                    && !texts.contains(value)) {

                texts.add(value);
            }
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {

                collectText(
                        child,
                        texts
                );

                child.recycle();
            }
        }
    }

    private void speak(String text) {

        if (textToSpeech == null) {
            return;
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "live_chat_reader"
        );
    }

    @Override
    public void onInterrupt() {

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    public void onDestroy() {

        if (textToSpeech != null) {

            textToSpeech.stop();
            textToSpeech.shutdown();

            textToSpeech = null;
        }

        super.onDestroy();
    }
    }
