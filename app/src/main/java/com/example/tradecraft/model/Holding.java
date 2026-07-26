package com.example.tradecraft.model;

/** Persisted POJO mirroring a users/{uid}/portfolio/{symbol} Firestore document. */
public class Holding {

    private String symbol;
    private long quantity;
    private double avgBuyPrice;

    public Holding() {
        // Required no-arg constructor for Firestore deserialization.
    }

    public Holding(String symbol, long quantity, double avgBuyPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
}
