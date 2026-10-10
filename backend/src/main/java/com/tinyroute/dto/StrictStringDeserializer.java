package com.tinyroute.dto;

import tools.jackson.core.*;
import tools.jackson.databind.*;

/** DTO-scoped rejection of scalar coercion; existing auth decoding stays unchanged. */
public final class StrictStringDeserializer extends ValueDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) return parser.getString();
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return context.reportInputMismatch(String.class, "Expected a JSON string.");
    }
}
