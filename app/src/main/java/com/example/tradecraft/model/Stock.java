package com.example.tradecraft.model;

/** App-level domain model for a single watchlist row on the Market screen. */
public class Stock {

    private String symbol;
    private String companyName;
    private String logoUrl;
    private double currentPrice;
    private double prevClose;

    public Stock() {
        // Required no-arg constructor to mirror the rest of the model layer.
    }

    public Stock(String symbol, String companyName, String logoUrl, double currentPrice, double prevClose) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.logoUrl = logoUrl;
        this.currentPrice = currentPrice;
        this.prevClose = prevClose;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(double prevClose) {
        this.prevClose = prevClose;
    }

    public double getPercentChange() {
        if (prevClose == 0) {
            return 0.0;
        }
        return (currentPrice - prevClose) / prevClose * 100;
    }
}
