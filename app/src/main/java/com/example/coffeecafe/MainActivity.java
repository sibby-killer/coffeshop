package com.example.coffeecafe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.auth.LoginActivity;
import com.example.coffeecafe.auth.SignupActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthManager authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            String role = authManager.getCurrentRole();
            Intent intent = new Intent(this, DashBoard.class);
            intent.putExtra("role", role);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Button getStartedBtn = findViewById(R.id.get_started_btn);
        Button loginBtn = findViewById(R.id.login_button);

        getStartedBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }
}
