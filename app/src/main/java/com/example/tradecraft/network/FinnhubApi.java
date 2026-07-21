package com.example.tradecraft.network;

import com.example.tradecraft.model.CompanyProfile;
import com.example.tradecraft.model.QuoteResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Finnhub REST endpoints used by the app. Base URL is configured on the Retrofit instance. */
public interface FinnhubApi {

    @GET("quote")
    Call<QuoteResponse> getQuote(@Query("symbol") String symbol, @Query("token") String token);

    @GET("stock/profile2")
    Call<CompanyProfile> getCompanyProfile(@Query("symbol") String symbol, @Query("token") String token);
}
