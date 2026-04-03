package com.g90.backend.modules.debt.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReminderListQuery {

    @Size(max = 36, message = "customerId must not exceed 36 characters")
    private String customerId;

    @Size(max = 50, message = "invoiceNumber must not exceed 50 characters")
    private String invoiceNumber;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @Size(max = 20, message = "reminderType must not exceed 20 characters")
    private String reminderType;

    @Size(max = 20, message = "channel must not exceed 20 characters")
    private String channel;

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private Integer pageSize = 20;
}
