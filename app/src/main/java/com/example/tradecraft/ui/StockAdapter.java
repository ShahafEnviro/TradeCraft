package com.example.tradecraft.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradecraft.R;
import com.example.tradecraft.model.Stock;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Binds the live watchlist to the Market screen's RecyclerView. */
public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final List<Stock> stocks = new ArrayList<>();

    public void submitList(List<Stock> newStocks) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new StockDiffCallback(stocks, newStocks));
        stocks.clear();
        stocks.addAll(newStocks);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        holder.bind(stocks.get(position), currencyFormat);
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        private final ImageView logo;
        private final TextView symbol;
        private final TextView companyName;
        private final TextView price;
        private final TextView percentChange;

        StockViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.stock_logo);
            symbol = itemView.findViewById(R.id.stock_symbol);
            companyName = itemView.findViewById(R.id.stock_company_name);
            price = itemView.findViewById(R.id.stock_price);
            percentChange = itemView.findViewById(R.id.stock_percent_change);
        }

        void bind(Stock stock, NumberFormat currencyFormat) {
            symbol.setText(stock.getSymbol());
            companyName.setText(stock.getCompanyName());
            price.setText(currencyFormat.format(stock.getCurrentPrice()));

            double percent = stock.getPercentChange();
            boolean isProfit = percent >= 0;
            percentChange.setText(String.format(Locale.US, "%+.2f%%", percent));
            percentChange.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isProfit ? R.color.profit_green : R.color.loss_red));

            String logoUrl = stock.getLogoUrl();
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Glide.with(logo).load(logoUrl).placeholder(R.drawable.ic_market).circleCrop().into(logo);
            } else {
                logo.setImageResource(R.drawable.ic_market);
            }
        }
    }

    private static class StockDiffCallback extends DiffUtil.Callback {
        private final List<Stock> oldList;
        private final List<Stock> newList;

        StockDiffCallback(List<Stock> oldList, List<Stock> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getSymbol().equals(newList.get(newItemPosition).getSymbol());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Stock oldStock = oldList.get(oldItemPosition);
            Stock newStock = newList.get(newItemPosition);
            return oldStock.getCurrentPrice() == newStock.getCurrentPrice()
                    && oldStock.getPrevClose() == newStock.getPrevClose();
        }
    }
}
