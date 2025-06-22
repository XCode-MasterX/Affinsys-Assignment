package com.restapi.taskManagement;

public class Balance {
    private float balance;
    private String currency;

    public float getBalance() { return balance; }
    public Balance setBalance(float balance) { this.balance = balance; return this;}

    public String getCurrency() { return currency; }
    public Balance setCurrency(String currency) { this.currency = currency; return this;}
}