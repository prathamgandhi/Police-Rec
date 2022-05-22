package xyz.prathamgandhi.cameratest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


public class SplashScreen extends AppCompatActivity {
    private final int SPLASH_DISPLAY_LENGTH = 500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        Handler h = new Handler();
        h.postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, HomePage.class);
            SplashScreen.this.startActivity(intent);
            SplashScreen.this.finish();
        }, SPLASH_DISPLAY_LENGTH);

    }
}