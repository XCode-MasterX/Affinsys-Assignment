package com.restapi.taskManagement;

import java.util.ArrayList;

// This works as well.
public class ActiveSessionHolder {
    public static ActiveSessionHolder instance;
    private ArrayList<String> activeSessions;

    private ActiveSessionHolder() {
        activeSessions = new ArrayList<>();
    }
    
    public static ActiveSessionHolder getInstance() { 
        if(instance == null)
            instance = new ActiveSessionHolder();
        return instance;
    }

    public boolean isSessionActive(String authToken) {
        return activeSessions.contains(authToken);
    }

    public boolean addSession(String authToken) {
        if(!activeSessions.contains(authToken)) {
            activeSessions.add(authToken);
            System.out.println(activeSessions);
            return true;
        }
        return false;
    }

    public boolean removeSession(String authToken) { return activeSessions.remove(authToken); }
}