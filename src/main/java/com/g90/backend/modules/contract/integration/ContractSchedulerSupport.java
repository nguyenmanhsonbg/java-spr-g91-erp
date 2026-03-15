package com.g90.backend.modules.contract.integration;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

public interface ContractSchedulerSupport {

    LocalDateTime nextAutoSubmitAt(LocalDateTime baseline);

    LocalDateTime approvalDueAt(LocalDateTime baseline);

    boolean shouldAutoSubmit(LocalDateTime dueAt, LocalDateTime now);

    boolean shouldAutoApprove(LocalDateTime dueAt, LocalDateTime now);
}

@Component
class DefaultContractSchedulerSupport implements ContractSchedulerSupport {

    @Override
    public LocalDateTime nextAutoSubmitAt(LocalDateTime baseline) {
        return baseline.plusDays(7);
    }

    @Override
    public LocalDateTime approvalDueAt(LocalDateTime baseline) {
        return baseline.plusHours(24);
    }

    @Override
    public boolean shouldAutoSubmit(LocalDateTime dueAt, LocalDateTime now) {
        return dueAt != null && now != null && !dueAt.isAfter(now);
    }

    @Override
    public boolean shouldAutoApprove(LocalDateTime dueAt, LocalDateTime now) {
        return dueAt != null && now != null && !dueAt.isAfter(now);
    }
}
