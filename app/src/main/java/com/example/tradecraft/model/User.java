package com.example.tradecraft.model;

/** Plain data object mirroring the Firestore document at users/{uid}. */
public class User {

    private String email;
    private double balance;

    public User() {
        // Required no-arg constructor for Firestore deserialization.
    }

    public User(String email, double balance) {
        this.email = email;
        this.balance = balance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
