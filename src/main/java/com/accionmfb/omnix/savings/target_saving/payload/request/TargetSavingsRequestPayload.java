/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.savings.target_saving.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Enyinna
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TargetSavingsRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;

    @NotNull(message = "Account number cannot be null")
    @NotEmpty(message = "Account number cannot be empty")
    @NotBlank(message = "Account number cannot be blank")
    @Pattern(regexp = "[0-9]{10}", message = "10 digit account number is required")
    private String accountNumber;

    @NotNull(message = "Goal name cannot be null")
    @NotEmpty(message = "Goal name cannot be empty")
    @NotBlank(message = "Goal name cannot be blank")
    private String goalName;

    @NotBlank(message = "Tenor is required")
    @Pattern(regexp = "^(3|6|9|12)$", message = "Tenor must be 3, 6, 9 or 12 months")
    @Schema(name = "Tenor", example = "3", description = "Tenor")
    private String tenor;

    @NotNull(message = "Frequency cannot be null")
    @NotEmpty(message = "Frequency cannot be empty")
    @NotBlank(message = "Frequency cannot be blank")
    @Pattern(regexp = "^(Daily|Weekly|Monthly)$", message = "Value must be either Daily, Weekly, Monthly")
    private String frequency;

    @NotBlank(message = "Savings amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Savings Amount must contain only digits, comma or dot only")
    @Schema(name = "Savings Amount", example = "1,000.00", description = "Savings Amount")
    private String savingsAmount;

    @NotNull(message = "Start date cannot be null")
    @NotEmpty(message = "Start date cannot be empty")
    @NotBlank(message = "Start date cannot be blank")
    @Pattern(regexp = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$", message = "Start date must be like 2021-01-13")
    private String startDate;

//    @NotBlank(message = "Hash value is required")
//    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;

    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}
