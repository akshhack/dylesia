package com.google.android.gms.samples.vision.ocrreader.Application;

import android.app.Application;

import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;

import com.google.android.gms.samples.vision.ocrreader.R;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs);

        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, fontRequest);

        EmojiCompat.init(config);
    }
}
