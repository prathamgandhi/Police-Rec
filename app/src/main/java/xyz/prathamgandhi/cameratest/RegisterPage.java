package xyz.prathamgandhi.cameratest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

public class RegisterPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        Button button = findViewById(R.id.button);

        button.setOnClickListener(view -> {
            
        });
    }
}