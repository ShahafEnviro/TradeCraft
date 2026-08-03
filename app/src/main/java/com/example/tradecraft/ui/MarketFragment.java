package com.example.tradecraft.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.tradecraft.R;
import com.example.tradecraft.model.Stock;
import com.example.tradecraft.viewmodel.MarketViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/** Live watchlist screen: a RecyclerView of stock quotes with pull-to-refresh. */
public class MarketFragment extends Fragment {

    private MarketViewModel viewModel;
    private final StockAdapter adapter = new StockAdapter();
    private List<Stock> allStocks = new ArrayList<>();

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView messageText;
    private TextInputEditText searchInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MarketViewModel.class);

        swipeRefresh = view.findViewById(R.id.market_swipe_refresh);
        progressBar = view.findViewById(R.id.market_progress_bar);
        messageText = view.findViewById(R.id.market_message_text);
        searchInput = view.findViewById(R.id.market_search_input);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterStocks(s != null ? s.toString() : "");
            }
        });

        RecyclerView recyclerView = view.findViewById(R.id.market_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnStockClickListener(stock ->
                TradeDialog.newInstance(stock.getSymbol(), stock.getCompanyName())
                        .show(getParentFragmentManager(), "trade"));

        // Refresh live quotes after a completed trade so cash/price changes are reflected in place.
        getParentFragmentManager().setFragmentResultListener(TradeDialog.RESULT_KEY,
                getViewLifecycleOwner(), (requestKey, result) -> viewModel.loadMarket());

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadMarket());

        observeViewModel();

        if (savedInstanceState == null) {
            viewModel.loadMarket();
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            swipeRefresh.setRefreshing(loading);
            progressBar.setVisibility(loading && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });

        viewModel.getStocks().observe(getViewLifecycleOwner(), stocks -> {
            allStocks = stocks != null ? stocks : new ArrayList<>();
            filterStocks(searchInput.getText() != null ? searchInput.getText().toString() : "");
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            messageText.setText(message);
            messageText.setVisibility(View.VISIBLE);
        });
    }

    private void filterStocks(String query) {
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.isEmpty()) {
            adapter.submitList(new ArrayList<>(allStocks));
            messageText.setVisibility(allStocks.isEmpty() ? View.VISIBLE : View.GONE);
            if (allStocks.isEmpty()) {
                messageText.setText(R.string.market_empty_state);
            }
            return;
        }

        List<Stock> filtered = new ArrayList<>();
        for (Stock stock : allStocks) {
            if (stock.getSymbol().toLowerCase().contains(lowerQuery) ||
                stock.getCompanyName().toLowerCase().contains(lowerQuery)) {
                filtered.add(stock);
            }
        }

        adapter.submitList(filtered);
        messageText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) {
            messageText.setText(R.string.market_empty_state);
        }
    }
}
