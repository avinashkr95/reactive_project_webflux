package com.reactive.ws.users.model;

public class AuthenticationRequest {
    private String email;
    private String password;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String password, String email) {
        this.password = password;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
