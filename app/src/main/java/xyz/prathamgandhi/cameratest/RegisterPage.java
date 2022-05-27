package xyz.prathamgandhi.cameratest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class RegisterPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        Button button = findViewById(R.id.submit);
        EditText first_name = findViewById(R.id.first_name),
                last_name = findViewById(R.id.last_name),
                password = findViewById(R.id.password),
                phone_num = findViewById(R.id.phone_num),
                station_id = findViewById(R.id.station_id);

        button.setOnClickListener(view -> {
            String firstName = first_name.getText().toString(),
                    lastName = last_name.getText().toString(),
                    passWord = password.getText().toString(),
                    phoneNum = phone_num.getText().toString(),
                    stationId = station_id.getText().toString();

            Intent intent = new Intent(this, RegisterPhoto.class);
            intent.putExtra("firstName", firstName);
            intent.putExtra("lastName", lastName);
            intent.putExtra("passWord", passWord);
            intent.putExtra("phoneNum", phoneNum);
            intent.putExtra("stationId", stationId);
            startActivity(intent);
        });
    }
}