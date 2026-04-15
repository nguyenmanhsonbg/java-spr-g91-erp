package com.g90.backend.modules.debt.service;

import com.g90.backend.modules.debt.entity.PaymentEntity;

public interface PaymentExecutionService {

    PaymentEntity recordPayment(PaymentExecutionCommand command);
}
