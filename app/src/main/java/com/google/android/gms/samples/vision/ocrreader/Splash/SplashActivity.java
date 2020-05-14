package com.google.android.gms.samples.vision.ocrreader.Splash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.samples.vision.ocrreader.Main.Main;
import com.google.android.gms.samples.vision.ocrreader.R;

public class SplashActivity extends AppCompatActivity {

    private static final int ANIMATION_TIME = 1000;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::startMainActivity, ANIMATION_TIME);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
