package com.example.tradecraft.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Single Retrofit instance for the Finnhub API. No other layer builds its own Retrofit client. */
public class RetrofitClient {

    private static final String BASE_URL = "https://finnhub.io/api/v1/";

    private static RetrofitClient instance;

    private final FinnhubApi api;

    private RetrofitClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(FinnhubApi.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public FinnhubApi getApi() {
        return api;
    }
}
