package com.tinyroute.dto;

public record PendingRegistrationResponse(String status) {

    public static PendingRegistrationResponse pendingVerification() {
        return new PendingRegistrationResponse("PENDING_VERIFICATION");
    }
}
