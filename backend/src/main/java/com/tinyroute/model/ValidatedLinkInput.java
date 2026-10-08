package com.tinyroute.model;

import java.time.Instant;

public record ValidatedLinkInput(
        DestinationUrl destinationUrl, ShortCode alias, Instant expiresAt) {}
