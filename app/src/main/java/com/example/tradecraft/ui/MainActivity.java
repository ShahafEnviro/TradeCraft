package com.example.tradecraft.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.AuthViewModel;

import java.text.NumberFormat;
import java.util.Locale;

/** Placeholder home screen: shows the logged-in user's email and starting balance. */
public class MainActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView welcomeText = findViewById(R.id.welcome_text);
        TextView balanceText = findViewById(R.id.balance_text);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        Button logoutButton = findViewById(R.id.logout_button);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getLoading().observe(this, isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                welcomeText.setText(getString(R.string.welcome_message, user.getEmail()));
                balanceText.setText(currencyFormat.format(user.getBalance()));
            }
        });

        viewModel.getError().observe(this, message -> {
            if (message != null) {
                welcomeText.setText(message);
            }
        });

        viewModel.getLoggedOut().observe(this, loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                goToLogin();
            }
        });

        logoutButton.setOnClickListener(v -> viewModel.logout());

        viewModel.loadCurrentUser();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
