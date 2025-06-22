package com.restapi.taskManagement;

import java.sql.Timestamp;

public class Transaction {
    private String kind;
    private float amount;
    private Timestamp timestamp;
    private float updated_bal;

    public void setUpdated_Bal(float bal) { this.updated_bal = bal; }
    public float getUpdated_Bal() { return updated_bal;}

    public void setKind(String kind) { this.kind = kind; }
    public String getKind() { return kind;}

    public void setAmount(float amt) { this.amount = amt; }
    public float getAmount() { return amount;}

    public void setTimestamp(Timestamp time) { this.timestamp = time; }
    public Timestamp getTimestamp() { return timestamp;}
}