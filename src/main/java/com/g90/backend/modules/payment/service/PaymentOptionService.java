package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import java.util.List;

public interface PaymentOptionService {

    List<PaymentOptionData> getPaymentOptions();
}
