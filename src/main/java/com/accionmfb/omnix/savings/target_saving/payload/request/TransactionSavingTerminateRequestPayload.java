package com.accionmfb.omnix.savings.target_saving.payload.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class TransactionSavingTerminateRequestPayload
{
    @NotNull(message = "accountNumber cannot be null")
    @NotEmpty(message = "accountNumber cannot be empty")
    @NotBlank(message = "accountNumber cannot be blank")
    @Pattern(regexp = "[0-9]{10}", message = "accountNumber must be 10 digit.")
    private String accountNumber;

    private String transactionType;
}
