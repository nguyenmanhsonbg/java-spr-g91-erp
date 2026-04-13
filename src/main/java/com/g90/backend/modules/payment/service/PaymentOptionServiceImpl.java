package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.entity.PaymentOptionEntity;
import com.g90.backend.modules.payment.repository.PaymentOptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOptionServiceImpl implements PaymentOptionService {

    private final PaymentOptionRepository paymentOptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOptionData> getPaymentOptions() {
        return paymentOptionRepository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                .map(this::toPaymentOptionData)
                .toList();
    }

    private PaymentOptionData toPaymentOptionData(PaymentOptionEntity entity) {
        return new PaymentOptionData(entity.getCode(), entity.getName(), entity.getDescription());
    }
}
