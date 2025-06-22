package com.restapi.taskManagement;

public class Response {
    protected int id;
    protected int status = 400;
    protected String error;
    protected String message;
    
    public int getId() { return id; }
    public Response setId(int id) { this.id = id; return this; }

    public int getStatus() { return status; }
    public Response setStatus(int status) { this.status = status; return this; }

    public String getError() { return error; }
    public Response setError(String error) { this.error = error; return this; }
    
    public String getMessage() { return message; }
    public Response setMessage(String message) { this.message = message; return this; }

    public String toString() {
        return String.format(
            "\"id\": %d,\n\"status\": %d,\n\"error\": %s,\n\"message\": %s", id, status, error, message
        );
    }
}