package com.g90.backend.modules.inventory.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.util.StringUtils;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String rawValue = parser.getValueAsString();
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        String value = rawValue.trim();
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // Fall through to date-only and offset-date-time parsing.
        }

        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // Fall through to offset-date-time parsing.
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            throw InvalidFormatException.from(
                    parser,
                    "receiptDate must use yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss",
                    value,
                    LocalDateTime.class
            );
        }
    }
}
