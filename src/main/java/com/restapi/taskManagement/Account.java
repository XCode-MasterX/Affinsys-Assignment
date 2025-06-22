package com.restapi.taskManagement;

import org.mindrot.jbcrypt.BCrypt;

public class Account {
    private String username;
    private String password;
    private String authToken = null;
    private float balance;

    public String getUsername() { return username; }
    public Account setUsername(String username) { this.username = username; authToken = null; return this; }

    public String getPassword() { return password; }
    public Account setPassword(String password) { this.password = password; authToken = null; return this; }

    public String getHashedPassword() { return hash(password); }

    public void setAuthToken(String auth) { authToken = auth; }
    public String getAuthToken() { 
        if(authToken == null) authToken = username + ":" + password;
        return authToken;
    }

    public void setBalance(float bal) { this.balance = bal; }
    public float getBalance() { return balance;}

    public boolean equals(Account x) {
        return this.authToken.equals(x.authToken);
    }

    public static String hash(Object o) { return hash(o.toString()); } 
    public static String hash(String toBeHashed) {
        return BCrypt.hashpw(toBeHashed, BCrypt.gensalt());
    }
}