package com.example.tradecraft.model;

import com.google.gson.annotations.SerializedName;

/** Wire model for Finnhub's GET /stock/profile2 response. */
public class CompanyProfile {

    @SerializedName("name")
    private String name;

    @SerializedName("ticker")
    private String ticker;

    @SerializedName("logo")
    private String logo;

    public CompanyProfile() {
        // Required no-arg constructor for Gson deserialization.
    }

    public String getName() {
        return name;
    }

    public String getTicker() {
        return ticker;
    }

    public String getLogo() {
        return logo;
    }
}
