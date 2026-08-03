package com.example.tradecraft.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradecraft.model.Holding;
import com.example.tradecraft.model.PortfolioPosition;
import com.example.tradecraft.model.Stock;
import com.example.tradecraft.model.User;
import com.example.tradecraft.repository.RepositoryCallback;
import com.example.tradecraft.repository.StockRepository;
import com.example.tradecraft.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mediates between PortfolioFragment and both repositories: UserRepository for cash balance and
 * holdings, StockRepository for live prices. Merges them into display-ready PortfolioPositions.
 */
public class PortfolioViewModel extends ViewModel {

    private final UserRepository userRepository = UserRepository.getInstance();
    private final StockRepository stockRepository = StockRepository.getInstance();

    private final MutableLiveData<Double> balanceLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PortfolioPosition>> positionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalValueLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalUnrealizedPlLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<Double> getBalance() {
        return balanceLiveData;
    }

    public LiveData<List<PortfolioPosition>> getPositions() {
        return positionsLiveData;
    }

    public LiveData<Double> getTotalValue() {
        return totalValueLiveData;
    }

    public LiveData<Double> getTotalUnrealizedPl() {
        return totalUnrealizedPlLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    /** Loads cash balance and holdings, then fetches a live quote per holding to build positions. */
    public void loadPortfolio() {
        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        userRepository.fetchCurrentUserProfile(new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                balanceLiveData.setValue(user.getBalance());
                loadHoldings(user.getBalance());
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    private void loadHoldings(double balance) {
        userRepository.getPortfolio(new RepositoryCallback<List<Holding>>() {
            @Override
            public void onSuccess(List<Holding> holdings) {
                if (holdings.isEmpty()) {
                    loadingLiveData.setValue(false);
                    positionsLiveData.setValue(new ArrayList<>());
                    totalValueLiveData.setValue(balance);
                    totalUnrealizedPlLiveData.setValue(0.0);
                    return;
                }
                fetchQuotesForHoldings(holdings, balance);
            }

            @Override
            public void onError(String errorMessage) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(errorMessage);
            }
        });
    }

    /** Fans out one live quote per holding; one bad symbol doesn't blank the whole portfolio. */
    private void fetchQuotesForHoldings(List<Holding> holdings, double balance) {
        Map<String, String> tickerNames = stockRepository.getMarketTickers();

        List<PortfolioPosition> positions = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(holdings.size());
        AtomicInteger failures = new AtomicInteger(0);

        for (Holding holding : holdings) {
            String companyName = tickerNames.getOrDefault(holding.getSymbol(), holding.getSymbol());
            stockRepository.getQuote(holding.getSymbol(), new RepositoryCallback<Stock>() {
                @Override
                public void onSuccess(Stock stock) {
                    positions.add(new PortfolioPosition(holding.getSymbol(), companyName,
                            holding.getQuantity(), holding.getAvgBuyPrice(), stock.getCurrentPrice()));
                    onOneFinished();
                }

                @Override
                public void onError(String errorMessage) {
                    failures.incrementAndGet();
                    onOneFinished();
                }

                private void onOneFinished() {
                    if (pending.decrementAndGet() == 0) {
                        finishLoading(positions, holdings, balance, failures.get());
                    }
                }
            });
        }
    }

    private void finishLoading(List<PortfolioPosition> positions, List<Holding> holdings, double balance, int failureCount) {
        loadingLiveData.setValue(false);

        if (failureCount > 0) {
            errorLiveData.setValue("Could not load live prices for your holdings. Please try again.");
            return;
        }

        sortByHoldingOrder(positions, holdings);

        double holdingsValue = 0;
        double totalPl = 0;
        for (PortfolioPosition position : positions) {
            holdingsValue += position.getMarketValue();
            totalPl += position.getUnrealizedPl();
        }

        positionsLiveData.setValue(positions);
        totalValueLiveData.setValue(balance + holdingsValue);
        totalUnrealizedPlLiveData.setValue(totalPl);
    }

    /** Keeps row order matching the holdings list regardless of the order quotes arrive in. */
    private void sortByHoldingOrder(List<PortfolioPosition> positions, List<Holding> holdings) {
        List<String> orderedSymbols = new ArrayList<>();
        for (Holding holding : holdings) {
            orderedSymbols.add(holding.getSymbol());
        }
        positions.sort((a, b) -> Integer.compare(
                orderedSymbols.indexOf(a.getSymbol()),
                orderedSymbols.indexOf(b.getSymbol())));
    }
}
