package com.restapi.taskManagement;

public class BuyResponse extends Response{
    private float balance;

    public int getId() { return id; }
    public BuyResponse setId(int id) { this.id = id; return this; }

    public int getStatus() { return status; }
    public BuyResponse setStatus(int status) { this.status = status; return this; }

    public String getError() { return error; }
    public BuyResponse setError(String error) { this.error = error; return this; }
    
    public String getMessage() { return message; }
    public BuyResponse setMessage(String message) { this.message = message; return this; }

    public BuyResponse setBalance(float balance) { this.balance = balance; return this; }
    public float getBalance() { return balance; }
}
