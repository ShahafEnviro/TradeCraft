package com.example.tradecraft.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradecraft.model.Stock;
import com.example.tradecraft.repository.RepositoryCallback;
import com.example.tradecraft.repository.StockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Mediates between MarketFragment and StockRepository, exposing the live watchlist as LiveData. */
public class MarketViewModel extends ViewModel {

    private final StockRepository repository = StockRepository.getInstance();

    private final MutableLiveData<List<Stock>> stocksLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<List<Stock>> getStocks() {
        return stocksLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    /** Fetches a fresh quote for every ticker in the watchlist; one bad symbol doesn't blank the list. */
    public void loadMarket() {
        Map<String, String> tickers = repository.getMarketTickers();
        List<String> symbols = new ArrayList<>(tickers.keySet());

        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        List<Stock> results = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(symbols.size());
        AtomicInteger failures = new AtomicInteger(0);

        for (String symbol : symbols) {
            repository.getQuote(symbol, new RepositoryCallback<Stock>() {
                @Override
                public void onSuccess(Stock result) {
                    results.add(result);
                    onOneFinished();
                }

                @Override
                public void onError(String errorMessage) {
                    failures.incrementAndGet();
                    onOneFinished();
                }

                private void onOneFinished() {
                    if (pending.decrementAndGet() == 0) {
                        loadingLiveData.setValue(false);
                        if (failures.get() > 0) {
                            errorLiveData.setValue("Could not load market data. Please try again.");
                        } else {
                            sortBySymbolOrder(results, symbols);
                            stocksLiveData.setValue(new ArrayList<>(results));
                        }
                    }
                }
            });
        }
    }

    /** Keeps row order matching the watchlist regardless of the order responses arrive in. */
    private void sortBySymbolOrder(List<Stock> stocks, List<String> orderedSymbols) {
        stocks.sort((a, b) -> Integer.compare(
                orderedSymbols.indexOf(a.getSymbol()),
                orderedSymbols.indexOf(b.getSymbol())));
    }
}
