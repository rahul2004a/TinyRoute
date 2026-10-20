package com.tinyroute.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_events")
public class ClickEvent {
    @Id private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "referrer_domain", length = 253)
    private String referrerDomain;

    @Column(nullable = false, length = 20)
    private String device;

    @Column(name = "operating_system", nullable = false, length = 20)
    private String operatingSystem;

    @Column(nullable = false, length = 30)
    private String browser;

    @Column(length = 2)
    private String country;

    @Column(length = 160)
    private String city;

    protected ClickEvent() {}
}
