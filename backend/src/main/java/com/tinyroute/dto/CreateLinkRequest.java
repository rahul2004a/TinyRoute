package com.tinyroute.dto;

import tools.jackson.databind.annotation.JsonDeserialize;

public record CreateLinkRequest(
        @JsonDeserialize(using = StrictStringDeserializer.class) String destinationUrl,
        @JsonDeserialize(using = StrictStringDeserializer.class) String alias,
        @JsonDeserialize(using = StrictStringDeserializer.class) String expiresAt) {}
