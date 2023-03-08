package com.accionmfb.omnix.savings.target_saving.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class SMSRequestPayload
{
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;

    @NotBlank(message = "NUBAN account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "NUBAN account number must be 10 digit")
    @Schema(name = "Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;

    @NotNull(message = "Message cannot be null")
    @NotEmpty(message = "Message cannot be empty")
    @NotBlank(message = "Message cannot be blank")
    private String message;

    @NotNull(message = "SMS reason be null")
    @NotEmpty(message = "SMS reason cannot be empty")
    @NotBlank(message = "SMS reason cannot be blank")
    private String smsFor;

    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;

    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;

    @NotNull(message = "SMS type be null")
    @NotEmpty(message = "SMS type cannot be empty")
    @NotBlank(message = "SMS type cannot be blank")
    @Pattern(regexp = "^(D|C|N)$", message = "SMS type required. D for Debit, C for Credit or N for None")
    private String smsType;

    @NotNull(message = "Token cannot be null")
    @NotEmpty(message = "Token cannot be empty")
    @NotBlank(message = "Token cannot be blank")
    private String token;

}
