package com.example.tradecraft.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.AuthViewModel;

import java.text.NumberFormat;
import java.util.Locale;

/** Placeholder Portfolio tab: shows the user's balance until trading lands in a later phase. */
public class PortfolioFragment extends Fragment {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        TextView welcomeText = view.findViewById(R.id.portfolio_welcome_text);
        TextView balanceText = view.findViewById(R.id.portfolio_balance_text);
        ProgressBar progressBar = view.findViewById(R.id.portfolio_progress_bar);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                welcomeText.setText(getString(R.string.welcome_message, user.getEmail()));
                balanceText.setText(currencyFormat.format(user.getBalance()));
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                welcomeText.setText(message);
            }
        });

        viewModel.loadCurrentUser();
    }
}
