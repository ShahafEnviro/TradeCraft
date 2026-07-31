package com.example.tradecraft.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradecraft.R;
import com.example.tradecraft.model.PortfolioPosition;
import com.example.tradecraft.viewmodel.PortfolioViewModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Portfolio dashboard: cash balance + live-valued holdings with unrealized P/L. */
public class PortfolioFragment extends Fragment {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final PortfolioAdapter adapter = new PortfolioAdapter();

    private PortfolioViewModel viewModel;

    private TextView totalValueText;
    private TextView cashText;
    private TextView holdingsValueText;
    private TextView plText;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView messageText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PortfolioViewModel.class);

        totalValueText = view.findViewById(R.id.portfolio_total_value_text);
        cashText = view.findViewById(R.id.portfolio_cash_text);
        holdingsValueText = view.findViewById(R.id.portfolio_holdings_value_text);
        plText = view.findViewById(R.id.portfolio_pl_text);
        progressBar = view.findViewById(R.id.portfolio_progress_bar);
        messageText = view.findViewById(R.id.portfolio_message_text);

        recyclerView = view.findViewById(R.id.portfolio_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        observeViewModel();

        if (savedInstanceState == null) {
            viewModel.loadPortfolio();
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) && adapter.getItemCount() == 0
                        ? View.VISIBLE : View.GONE));

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                cashText.setText(currencyFormat.format(balance));
            }
        });

        viewModel.getTotalValue().observe(getViewLifecycleOwner(), totalValue -> {
            if (totalValue != null) {
                totalValueText.setText(currencyFormat.format(totalValue));
            }
        });

        viewModel.getTotalUnrealizedPl().observe(getViewLifecycleOwner(), this::bindTotalPl);

        viewModel.getPositions().observe(getViewLifecycleOwner(), this::bindPositions);

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            recyclerView.setVisibility(View.GONE);
            messageText.setText(message);
            messageText.setVisibility(View.VISIBLE);
        });
    }

    private void bindPositions(List<PortfolioPosition> positions) {
        adapter.submitList(positions);

        double holdingsValue = 0;
        for (PortfolioPosition position : positions) {
            holdingsValue += position.getMarketValue();
        }
        holdingsValueText.setText(currencyFormat.format(holdingsValue));

        boolean isEmpty = positions.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            messageText.setText(R.string.portfolio_empty_state);
            messageText.setVisibility(View.VISIBLE);
        } else {
            messageText.setVisibility(View.GONE);
        }
    }

    private void bindTotalPl(Double totalPl) {
        if (totalPl == null) {
            return;
        }
        boolean isProfit = totalPl >= 0;
        plText.setText(String.format(Locale.US, "%s%s",
                isProfit ? "+" : "-", currencyFormat.format(Math.abs(totalPl))));
        plText.setTextColor(ContextCompat.getColor(requireContext(),
                isProfit ? R.color.profit_green : R.color.loss_red));
    }
}
