package com.example.coffeecafe.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coffeecafe.R;

public class EmailVerifiedActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verified);

        TextView title = findViewById(R.id.verified_title);
        TextView message = findViewById(R.id.verified_message);
        Button loginBtn = findViewById(R.id.go_to_login_btn);

        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
