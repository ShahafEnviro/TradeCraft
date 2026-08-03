package com.example.tradecraft.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradecraft.R;
import com.example.tradecraft.model.PortfolioPosition;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Binds merged holdings + live-price positions to the Portfolio screen's RecyclerView. */
public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.PositionViewHolder> {

    /** Notified when a holding row is tapped, so the host can open the trade dialog. */
    public interface OnPositionClickListener {
        void onPositionClick(PortfolioPosition position);
    }

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final List<PortfolioPosition> positions = new ArrayList<>();
    private OnPositionClickListener clickListener;

    public void setOnPositionClickListener(OnPositionClickListener listener) {
        this.clickListener = listener;
    }

    public void submitList(List<PortfolioPosition> newPositions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PositionDiffCallback(positions, newPositions));
        positions.clear();
        positions.addAll(newPositions);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_holding, parent, false);
        return new PositionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PositionViewHolder holder, int position) {
        PortfolioPosition portfolioPosition = positions.get(position);
        holder.bind(portfolioPosition, currencyFormat);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPositionClick(portfolioPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return positions.size();
    }

    static class PositionViewHolder extends RecyclerView.ViewHolder {
        private final TextView symbol;
        private final TextView companyName;
        private final TextView quantity;
        private final TextView currentPrice;
        private final TextView marketValue;
        private final TextView pl;

        PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.holding_symbol);
            companyName = itemView.findViewById(R.id.holding_company_name);
            quantity = itemView.findViewById(R.id.holding_quantity);
            currentPrice = itemView.findViewById(R.id.holding_current_price);
            marketValue = itemView.findViewById(R.id.holding_market_value);
            pl = itemView.findViewById(R.id.holding_pl);
        }

        void bind(PortfolioPosition position, NumberFormat currencyFormat) {
            symbol.setText(position.getSymbol());
            companyName.setText(position.getCompanyName());
            quantity.setText(itemView.getContext().getString(R.string.holding_quantity_format,
                    position.getQuantity(), currencyFormat.format(position.getAvgBuyPrice())));
            currentPrice.setText(currencyFormat.format(position.getCurrentPrice()));
            marketValue.setText(currencyFormat.format(position.getMarketValue()));

            double unrealizedPl = position.getUnrealizedPl();
            boolean isProfit = unrealizedPl >= 0;
            pl.setText(String.format(Locale.US, "%s%s (%+.2f%%)",
                    isProfit ? "+" : "-", currencyFormat.format(Math.abs(unrealizedPl)), position.getUnrealizedPlPercent()));
            pl.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isProfit ? R.color.profit_green : R.color.loss_red));
        }
    }

    private static class PositionDiffCallback extends DiffUtil.Callback {
        private final List<PortfolioPosition> oldList;
        private final List<PortfolioPosition> newList;

        PositionDiffCallback(List<PortfolioPosition> oldList, List<PortfolioPosition> newList) {
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
            PortfolioPosition oldPosition = oldList.get(oldItemPosition);
            PortfolioPosition newPosition = newList.get(newItemPosition);
            return oldPosition.getQuantity() == newPosition.getQuantity()
                    && oldPosition.getAvgBuyPrice() == newPosition.getAvgBuyPrice()
                    && oldPosition.getCurrentPrice() == newPosition.getCurrentPrice();
        }
    }
}
