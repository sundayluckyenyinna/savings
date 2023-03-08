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
@AllArgsConstructor
@NoArgsConstructor
public class TargetSavingTerminationRequestPayload
{
//    @NotNull(message = "Target Savings Id cannot be null")
//    @NotEmpty(message = "Target Savings Id cannot be empty")
//    @NotBlank(message = "Target Savings Id cannot be blank")
//    @Pattern(regexp = "[0-9]{1,}", message = "Numerical target savings Id required")
    private String goalName;

    private String targetSavingsId;

    private String accountNumber;
//    @NotBlank(message = "Hash value is required")
//    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;

    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
