package com.g90.backend.modules.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @NotBlank(message = "Tax code is required")
    @Pattern(regexp = "^\\d{10,13}$", message = "Tax code must be 10-13 digits")
    private String taxCode;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 255, message = "Contact person must not exceed 255 characters")
    private String contactPerson;

    @Pattern(regexp = "^(|[0-9+\\-()\\s]{8,20})$", message = "Phone must be 8-20 characters and contain only digits or common phone symbols")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Customer type is required")
    @Size(max = 50, message = "Customer type must not exceed 50 characters")
    private String customerType;

    @Size(max = 50, message = "Price group must not exceed 50 characters")
    private String priceGroup;

    @DecimalMin(value = "0.00", message = "Credit limit must be greater than or equal to 0")
    private BigDecimal creditLimit;

    @Size(max = 255, message = "Payment terms must not exceed 255 characters")
    private String paymentTerms;

    @Size(max = 1000, message = "Change reason must not exceed 1000 characters")
    private String changeReason;
}
