package com.tinyroute.client;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public final class RegistrationMailCapacity {

    public static final int MAX_WORKERS = 2;
    public static final int QUEUE_CAPACITY = 100;
    public static final int MAX_PENDING_DELIVERIES = MAX_WORKERS + QUEUE_CAPACITY;

    private final Semaphore availableDeliveries = new Semaphore(MAX_PENDING_DELIVERIES);

    public boolean tryReserve() {
        return availableDeliveries.tryAcquire();
    }

    public void release() {
        availableDeliveries.release();
    }
}
