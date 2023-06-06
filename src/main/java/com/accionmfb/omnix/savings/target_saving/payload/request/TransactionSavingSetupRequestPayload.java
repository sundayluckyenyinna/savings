package com.accionmfb.omnix.savings.target_saving.payload.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class TransactionSavingSetupRequestPayload
{
    @NotNull(message = "accountNumber cannot be null")
    @NotEmpty(message = "accountNumber cannot be empty")
    @NotBlank(message = "accountNumber cannot be blank")
    private String accountNumber;

    @NotNull(message = "transactionType cannot be null")
    @NotEmpty(message = "transactionType cannot be empty")
    @NotBlank(message = "transactionType cannot be blank")
    @Pattern(regexp = "^(FT_TRANSFER|AIRTIME_PURCHASE|DATA_PURCHASE|BILL_PAYMENT)$", message = "transactionType can only be of FT_TRANSFER, AIRTIME_PURCHASE, DATA_PURCHASE or BILL_PAYMENT")
    private String transactionType;

    @NotNull(message = "savePercent cannot be null")
    @NotEmpty(message = "savePercent cannot be empty")
    @NotBlank(message = "savePercent cannot be blank")
    @Pattern(regexp = "[0-9]*['.']?[0-9]*", message = "Saving percent must contain only digit")
    private String savePercent;

    @NotNull(message = "requestId cannot be null")
    @NotEmpty(message = "requestId cannot be empty")
    @NotBlank(message = "requestId cannot be blank")
    private String requestId;
}
