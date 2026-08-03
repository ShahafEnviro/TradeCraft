package com.example.tradecraft.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradecraft.model.Holding;
import com.example.tradecraft.model.Stock;
import com.example.tradecraft.model.User;
import com.example.tradecraft.repository.RepositoryCallback;
import com.example.tradecraft.repository.StockRepository;
import com.example.tradecraft.repository.UserRepository;

/**
 * Orchestrates a single buy/sell for the Trade dialog: pulls context (live price, cash, owned
 * shares) from both repositories, then executes trades against a freshly re-fetched live price so
 * the client can never dictate the traded price (PLAN §4). The two repositories never call each
 * other — this ViewModel is the seam that joins StockRepository and UserRepository.
 */
public class TradeViewModel extends ViewModel {

    private final StockRepository stockRepository = StockRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private final MutableLiveData<Double> livePriceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> cashBalanceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Long> ownedQuantityLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> tradeSuccessLiveData = new MutableLiveData<>();

    public LiveData<Double> getLivePrice() {
        return livePriceLiveData;
    }

    public LiveData<Double> getCashBalance() {
        return cashBalanceLiveData;
    }

    public LiveData<Long> getOwnedQuantity() {
        return ownedQuantityLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<String> getTradeSuccess() {
        return tradeSuccessLiveData;
    }

    /** Loads the live quote, cash balance, and currently-owned quantity to populate the dialog. */
    public void loadContext(String symbol) {
        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        stockRepository.getQuote(symbol, new RepositoryCallback<Stock>() {
            @Override
            public void onSuccess(Stock stock) {
                livePriceLiveData.setValue(stock.getCurrentPrice());
                loadingLiveData.setValue(false);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });

        userRepository.fetchCurrentUserProfile(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                cashBalanceLiveData.setValue(user.getBalance());
            }

            @Override
            public void onError(String errorMessage) {
                errorLiveData.setValue(errorMessage);
            }
        });

        userRepository.getHolding(symbol, new RepositoryCallback<Holding>() {
            @Override
            public void onSuccess(Holding holding) {
                ownedQuantityLiveData.setValue(holding != null ? holding.getQuantity() : 0L);
            }

            @Override
            public void onError(String errorMessage) {
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    /** Re-fetches the live price, then buys at that fresh price — never the displayed one. */
    public void buy(String symbol, long quantity) {
        executeWithFreshPrice(symbol, quantity, true);
    }

    /** Re-fetches the live price, then sells at that fresh price — never the displayed one. */
    public void sell(String symbol, long quantity) {
        executeWithFreshPrice(symbol, quantity, false);
    }

    private void executeWithFreshPrice(String symbol, long quantity, boolean isBuy) {
        if (quantity <= 0) {
            errorLiveData.setValue("Enter a quantity greater than zero.");
            return;
        }

        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        stockRepository.getQuote(symbol, new RepositoryCallback<Stock>() {
            @Override
            public void onSuccess(Stock stock) {
                double price = stock.getCurrentPrice();
                livePriceLiveData.setValue(price);
                if (price <= 0) {
                    // Finnhub returns 0 when the market is closed or the symbol has no data.
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("No live price available right now. Please try again later.");
                    return;
                }
                if (isBuy) {
                    executeBuy(symbol, quantity, price);
                } else {
                    executeSell(symbol, quantity, price);
                }
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    private void executeBuy(String symbol, long quantity, double price) {
        userRepository.executeBuy(symbol, quantity, price, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadingLiveData.setValue(false);
                tradeSuccessLiveData.setValue("Bought " + quantity + " " + symbol);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    private void executeSell(String symbol, long quantity, double price) {
        userRepository.executeSell(symbol, quantity, price, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadingLiveData.setValue(false);
                tradeSuccessLiveData.setValue("Sold " + quantity + " " + symbol);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }
}
