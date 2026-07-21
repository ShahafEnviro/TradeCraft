package com.example.tradecraft.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.AuthViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** App shell: a toolbar (with logout) and a bottom nav hosting the Market and Portfolio tabs. */
public class MainActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.main_bottom_nav);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                viewModel.logout();
                return true;
            }
            return false;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            showFragment(item.getItemId() == R.id.nav_portfolio ? new PortfolioFragment() : new MarketFragment());
            return true;
        });

        viewModel.getLoggedOut().observe(this, loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                goToLogin();
            }
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_market);
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
