package com.tinyroute.model;

public record CreateLinkCommand(String destinationUrl, String alias, String expiresAt) {}
