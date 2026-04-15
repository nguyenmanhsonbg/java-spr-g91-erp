package com.g90.backend.modules.payment.service;

import com.g90.backend.modules.payment.config.PaymentBankTransferProperties;
import com.g90.backend.modules.payment.dto.InvoiceResponse;
import com.g90.backend.modules.payment.dto.PaymentInstructionResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentInstructionServiceImpl implements PaymentInstructionService {

    private final InvoiceService invoiceService;
    private final PaymentBankTransferProperties bankTransferProperties;

    public PaymentInstructionServiceImpl(
            InvoiceService invoiceService,
            PaymentBankTransferProperties bankTransferProperties
    ) {
        this.invoiceService = invoiceService;
        this.bankTransferProperties = bankTransferProperties;
    }

    @Override
    public PaymentInstructionResponse getPaymentInstruction(String invoiceId) {
        InvoiceResponse invoice = invoiceService.getInvoice(invoiceId);
        String transferContent = "PAY-" + invoice.invoiceNumber();
        return new PaymentInstructionResponse(
                "BANK_TRANSFER_QR",
                invoice.id(),
                invoice.invoiceNumber(),
                invoice.customerId(),
                invoice.grandTotal(),
                invoice.paidAmount(),
                invoice.outstandingAmount(),
                bankTransferProperties.getBankName(),
                bankTransferProperties.getAccountName(),
                bankTransferProperties.getAccountNumber(),
                transferContent,
                buildQrContent(invoice, transferContent),
                null
        );
    }

    private String buildQrContent(InvoiceResponse invoice, String transferContent) {
        return "BANK_TRANSFER_QR"
                + "|bankName=" + bankTransferProperties.getBankName()
                + "|accountName=" + bankTransferProperties.getAccountName()
                + "|accountNumber=" + bankTransferProperties.getAccountNumber()
                + "|amount=" + invoice.outstandingAmount()
                + "|content=" + transferContent;
    }
}
