package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class QuotationAmountTooLowException extends ApiException {

    public QuotationAmountTooLowException() {
        super(
                HttpStatus.BAD_REQUEST,
                "QUOTATION_AMOUNT_TOO_LOW",
                "Quotation total amount must be at least 10,000,000 VND",
                List.of(ValidationErrorItem.builder()
                        .field("totalAmount")
                        .message("Quotation total amount must be at least 10,000,000 VND")
                        .build())
        );
    }
}
