package com.g90.backend.modules.contract.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ContractSubmitRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledSubmissionAt;

    @Size(max = 500, message = "Submission note must not exceed 500 characters")
    private String submissionNote;
}
