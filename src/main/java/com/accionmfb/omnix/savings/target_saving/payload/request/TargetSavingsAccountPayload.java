package com.accionmfb.omnix.savings.target_saving.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetSavingsAccountPayload
{
    @NotNull(message = "Account number cannot be null")
    @NotEmpty(message = "Account number cannot be empty")
    @NotBlank(message = "Account number cannot be blank")
    @Pattern(regexp = "[0-9]{10}", message = "10 digit account number is required")
    private String accountNumber;

    private String hash;

    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;

    private String imei;
}
