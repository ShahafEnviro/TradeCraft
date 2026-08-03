package com.example.tradecraft.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradecraft.R;
import com.example.tradecraft.viewmodel.TradeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Buy/sell dialog for a single symbol. Owns a TradeViewModel that fetches the live price and cash
 * context, then executes trades against a freshly re-fetched price. On success it signals the host
 * screen via the Fragment Result API so the market/portfolio list can refresh.
 */
public class TradeDialog extends DialogFragment {

    /** Fragment-result key the host fragments listen on to refresh after a completed trade. */
    public static final String RESULT_KEY = "trade_completed";

    private static final String ARG_SYMBOL = "arg_symbol";
    private static final String ARG_COMPANY_NAME = "arg_company_name";

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    private TradeViewModel viewModel;
    private String symbol;
    private Double currentPrice;

    private TextView priceText;
    private ProgressBar priceProgress;
    private TextView cashText;
    private TextView ownedText;
    private TextInputEditText quantityInput;
    private TextView estimateText;
    private TextView errorText;
    private MaterialButton buyButton;
    private MaterialButton sellButton;
    private ProgressBar progressBar;

    public static TradeDialog newInstance(String symbol, String companyName) {
        TradeDialog dialog = new TradeDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SYMBOL, symbol);
        args.putString(ARG_COMPANY_NAME, companyName);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_trade, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        symbol = args.getString(ARG_SYMBOL);
        String companyName = args.getString(ARG_COMPANY_NAME);

        viewModel = new ViewModelProvider(this).get(TradeViewModel.class);

        ((TextView) view.findViewById(R.id.trade_symbol)).setText(symbol);
        ((TextView) view.findViewById(R.id.trade_company_name)).setText(companyName);

        priceText = view.findViewById(R.id.trade_current_price);
        priceProgress = view.findViewById(R.id.trade_price_progress);
        cashText = view.findViewById(R.id.trade_cash_available);
        ownedText = view.findViewById(R.id.trade_owned);
        quantityInput = view.findViewById(R.id.trade_quantity_input);
        estimateText = view.findViewById(R.id.trade_estimate);
        errorText = view.findViewById(R.id.trade_error);
        buyButton = view.findViewById(R.id.trade_buy_button);
        sellButton = view.findViewById(R.id.trade_sell_button);
        progressBar = view.findViewById(R.id.trade_progress_bar);

        priceProgress.setVisibility(View.VISIBLE);
        updateEstimate();

        quantityInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateEstimate();
            }
        });

        buyButton.setOnClickListener(v -> viewModel.buy(symbol, parseQuantity()));
        sellButton.setOnClickListener(v -> viewModel.sell(symbol, parseQuantity()));

        observeViewModel();

        if (savedInstanceState == null) {
            viewModel.loadContext(symbol);
        }
    }

    private void observeViewModel() {
        viewModel.getLivePrice().observe(getViewLifecycleOwner(), price -> {
            currentPrice = price;
            priceProgress.setVisibility(View.GONE);
            priceText.setText(price != null ? currencyFormat.format(price)
                    : getString(R.string.trade_price_placeholder));
            updateEstimate();
        });

        viewModel.getCashBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                cashText.setText(currencyFormat.format(balance));
            }
        });

        viewModel.getOwnedQuantity().observe(getViewLifecycleOwner(), owned ->
                ownedText.setText(getString(R.string.trade_owned_format, owned != null ? owned : 0L)));

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            buyButton.setEnabled(!loading);
            sellButton.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                errorText.setVisibility(View.GONE);
                return;
            }
            errorText.setText(message);
            errorText.setVisibility(View.VISIBLE);
        });

        viewModel.getTradeSuccess().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
            dismiss();
        });
    }

    /** Recomputes the estimated total (price × quantity) from the latest price and quantity input. */
    private void updateEstimate() {
        double price = currentPrice != null ? currentPrice : 0.0;
        double total = price * parseQuantity();
        estimateText.setText(getString(R.string.trade_estimate_format, currencyFormat.format(total)));
    }

    /** Parses the quantity field; returns 0 for empty/invalid input so the ViewModel rejects it. */
    private long parseQuantity() {
        String raw = quantityInput.getText() != null ? quantityInput.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Convenience adapter so the quantity watcher only overrides the one method it needs. */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
