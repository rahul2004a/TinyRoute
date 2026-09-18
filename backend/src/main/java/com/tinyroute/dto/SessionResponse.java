package com.tinyroute.dto;

public record SessionResponse(boolean authenticated, UserResponse user) {

    public static SessionResponse authenticated(String email) {
        return new SessionResponse(true, new UserResponse(email));
    }

    public record UserResponse(String email) {
    }
}
