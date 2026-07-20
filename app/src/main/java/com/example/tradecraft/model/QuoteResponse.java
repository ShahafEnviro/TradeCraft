package com.example.tradecraft.model;

import com.google.gson.annotations.SerializedName;

/** Wire model for Finnhub's GET /quote response. */
public class QuoteResponse {

    @SerializedName("c")
    private double current;

    @SerializedName("h")
    private double high;

    @SerializedName("l")
    private double low;

    @SerializedName("o")
    private double open;

    @SerializedName("pc")
    private double previousClose;

    public QuoteResponse() {
        // Required no-arg constructor for Gson deserialization.
    }

    public double getCurrent() {
        return current;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getOpen() {
        return open;
    }

    public double getPreviousClose() {
        return previousClose;
    }
}
