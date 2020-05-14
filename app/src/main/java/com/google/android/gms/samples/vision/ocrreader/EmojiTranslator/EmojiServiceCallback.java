package com.google.android.gms.samples.vision.ocrreader.EmojiTranslator;

public interface EmojiServiceCallback {

    void onSuccess(String translation);
    void onFailure();
}
