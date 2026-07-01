package com.example.tradecraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.AuthViewModel;

/** Entry point of the app: email/password login and sign-up. */
public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private EditText emailInput;
    private EditText passwordInput;
    private ProgressBar progressBar;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (viewModel.isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);

        Button loginButton = findViewById(R.id.login_button);
        Button signUpButton = findViewById(R.id.signup_button);

        loginButton.setOnClickListener(v -> submit(false));
        signUpButton.setOnClickListener(v -> submit(true));

        observeViewModel();
    }

    private void submit(boolean isSignUp) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        errorText.setVisibility(View.GONE);
        if (isSignUp) {
            viewModel.signUp(email, password);
        } else {
            viewModel.login(email, password);
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(this, this::showError);

        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                goToMain();
            }
        });
    }

    private void showError(String message) {
        if (message == null) {
            return;
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
