package com.example.tradecraft.model;

/** UI-only display model combining a Holding with its live price; never persisted to Firestore. */
public class PortfolioPosition {

    private String symbol;
    private String companyName;
    private long quantity;
    private double avgBuyPrice;
    private double currentPrice;

    public PortfolioPosition() {
        // Required no-arg constructor to mirror the rest of the model layer.
    }

    public PortfolioPosition(String symbol, String companyName, long quantity, double avgBuyPrice, double currentPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.currentPrice = currentPrice;
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

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public double getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public void setAvgBuyPrice(double avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getMarketValue() {
        return currentPrice * quantity;
    }

    public double getUnrealizedPl() {
        return (currentPrice - avgBuyPrice) * quantity;
    }

    public double getUnrealizedPlPercent() {
        if (avgBuyPrice == 0) {
            return 0.0;
        }
        return (currentPrice - avgBuyPrice) / avgBuyPrice * 100;
    }
}
