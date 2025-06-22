package com.restapi.taskManagement;

import java.util.HashMap;

// This works as well.
public class ActiveSessionHolder {
    public static ActiveSessionHolder instance;
    private HashMap<String, Account> activeSessions;

    private ActiveSessionHolder() {
        activeSessions = new HashMap<>();
    }
    
    public static ActiveSessionHolder getInstance() { 
        if(instance == null)
            instance = new ActiveSessionHolder();
        return instance;
    }

    public boolean isSessionActive(String authToken) {
        return activeSessions.containsKey(authToken);
    }

    public boolean addSession(String authToken, Account account) {
        if(!activeSessions.containsKey(authToken)) {
            activeSessions.put(authToken, account);
            //System.out.println(activeSessions);
            return true;
        }
        return false;
    }
    
    public Account getAccount(String authToken) { return activeSessions.get(authToken); }

    public boolean removeSession(String authToken) { return activeSessions.remove(authToken) != null; }
}