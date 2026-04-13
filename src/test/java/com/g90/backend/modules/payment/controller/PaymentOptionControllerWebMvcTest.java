package com.g90.backend.modules.payment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.g90.backend.modules.account.repository.UserAccountRepository;
import com.g90.backend.modules.payment.dto.PaymentOptionData;
import com.g90.backend.modules.payment.service.PaymentOptionService;
import com.g90.backend.security.AccessTokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentOptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentOptionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentOptionService paymentOptionService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void canGetPaymentOptions() throws Exception {
        when(paymentOptionService.getPaymentOptions()).thenReturn(List.of(
                new PaymentOptionData("PREPAID_100", "Thanh toan 100%", "Thanh toan toan bo truoc khi giao"),
                new PaymentOptionData("DEPOSIT_30", "Dat coc 30%", "Dat coc 30%, thanh toan phan con lai khi giao")
        ));

        mockMvc.perform(get("/api/payment-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Payment options fetched successfully"))
                .andExpect(jsonPath("$.data[0].code").value("PREPAID_100"))
                .andExpect(jsonPath("$.data[1].code").value("DEPOSIT_30"));
    }
}
