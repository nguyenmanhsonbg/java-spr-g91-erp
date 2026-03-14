package com.g90.backend.exception;

import com.g90.backend.dto.ValidationErrorItem;
import java.util.List;
import org.springframework.http.HttpStatus;

public class PromotionNotApplicableException extends ApiException {

    public PromotionNotApplicableException(String promotionCode) {
        super(
                HttpStatus.BAD_REQUEST,
                "PROMOTION_NOT_APPLICABLE",
                "Promotion code is invalid or not applicable",
                List.of(ValidationErrorItem.builder()
                        .field("promotionCode")
                        .message("Promotion cannot be applied: " + promotionCode)
                        .build())
        );
    }
}
