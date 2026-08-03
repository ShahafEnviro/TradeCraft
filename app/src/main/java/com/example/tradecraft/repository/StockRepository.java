package com.example.tradecraft.repository;

import com.example.tradecraft.BuildConfig;
import com.example.tradecraft.model.QuoteResponse;
import com.example.tradecraft.model.Stock;
import com.example.tradecraft.network.FinnhubApi;
import com.example.tradecraft.network.RetrofitClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single source of truth for live market data. Wraps the Finnhub Retrofit API so no other layer
 * touches Retrofit directly.
 */
public class StockRepository {

    /** MVP watchlist: Finnhub's free tier has no reliable symbol search, so the tickers are fixed. */
    private static final Map<String, String> MARKET_TICKERS = new LinkedHashMap<>();

    static {
        MARKET_TICKERS.put("AAPL", "Apple Inc.");
        MARKET_TICKERS.put("MSFT", "Microsoft Corporation");
        MARKET_TICKERS.put("GOOGL", "Alphabet Inc.");
        MARKET_TICKERS.put("AMZN", "Amazon.com, Inc.");
        MARKET_TICKERS.put("TSLA", "Tesla, Inc.");
        MARKET_TICKERS.put("META", "Meta Platforms, Inc.");
        MARKET_TICKERS.put("NVDA", "NVIDIA Corporation");
        MARKET_TICKERS.put("NFLX", "Netflix, Inc.");
        MARKET_TICKERS.put("AMD", "Advanced Micro Devices, Inc.");
        MARKET_TICKERS.put("JPM", "JPMorgan Chase & Co.");
        MARKET_TICKERS.put("DIS", "The Walt Disney Company");
        MARKET_TICKERS.put("KO", "The Coca-Cola Company");
    }

    private static StockRepository instance;

    private final FinnhubApi api;
    
    private final Map<String, CachedStock> cache = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 15000; // 15 seconds

    private static class CachedStock {
        final Stock stock;
        final long fetchTimeMs;

        CachedStock(Stock stock, long fetchTimeMs) {
            this.stock = stock;
            this.fetchTimeMs = fetchTimeMs;
        }
    }

    private StockRepository() {
        api = RetrofitClient.getInstance().getApi();
    }

    public static synchronized StockRepository getInstance() {
        if (instance == null) {
            instance = new StockRepository();
        }
        return instance;
    }

    /** The fixed watchlist shown on the Market screen, symbol to display name. */
    public Map<String, String> getMarketTickers() {
        return MARKET_TICKERS;
    }

    /** Fetches a live quote for one symbol and maps it onto the app-level Stock model. */
    public void getQuote(String symbol, RepositoryCallback<Stock> callback) {
        String companyName = MARKET_TICKERS.getOrDefault(symbol, symbol);
        
        CachedStock cached = cache.get(symbol);
        if (cached != null && System.currentTimeMillis() - cached.fetchTimeMs < CACHE_EXPIRY_MS) {
            callback.onSuccess(cached.stock);
            return;
        }

        api.getQuote(symbol, BuildConfig.FINNHUB_API_KEY).enqueue(new Callback<QuoteResponse>() {
            @Override
            public void onResponse(Call<QuoteResponse> call, Response<QuoteResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (cached != null) {
                        callback.onSuccess(cached.stock);
                    } else {
                        callback.onError("Could not load a quote for " + symbol + ".");
                    }
                    return;
                }
                QuoteResponse quote = response.body();
                Stock stock = new Stock(symbol, companyName, null, quote.getCurrent(), quote.getPreviousClose());
                cache.put(symbol, new CachedStock(stock, System.currentTimeMillis()));
                callback.onSuccess(stock);
            }

            @Override
            public void onFailure(Call<QuoteResponse> call, Throwable t) {
                if (cached != null) {
                    callback.onSuccess(cached.stock);
                } else {
                    callback.onError("Network error loading " + symbol + ". Please try again.");
                }
            }
        });
    }
}
