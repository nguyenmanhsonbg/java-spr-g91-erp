package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractDocumentEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be valid")
    @Size(max = 255, message = "Recipient email must not exceed 255 characters")
    private String recipientEmail;
}
