package com.tinyroute.dto;

public record GenericAcceptedResponse(String status) {

    public static GenericAcceptedResponse accepted() {
        return new GenericAcceptedResponse("ACCEPTED");
    }
}
