package com.g90.backend.modules.debt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReminderCreateRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotEmpty(message = "At least one invoiceId is required")
    private List<@NotBlank(message = "invoiceIds must not contain blank values") String> invoiceIds;

    @NotBlank(message = "reminderType is required")
    @Size(max = 20, message = "reminderType must not exceed 20 characters")
    private String reminderType;

    @NotBlank(message = "channel is required")
    @Size(max = 20, message = "channel must not exceed 20 characters")
    private String channel;

    @Size(max = 1000, message = "message must not exceed 1000 characters")
    private String message;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
