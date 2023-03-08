package com.accionmfb.omnix.savings.target_saving.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundsTransferPayload
{
    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;

    @NotNull(message = "Branch code cannot be null")
    @NotEmpty(message = "Branch code cannot be empty")
    @NotBlank(message = "Branch code cannot be blank")
    @Pattern(regexp = "^[A-Za-z]{2}[0-9]{7}$", message = "Branch code like BB0010000 required")
    private String branchCode;

    @NotBlank(message = "Debit account is required")
    @Pattern(regexp = "^[0-9]{11}|(NGN1([0-9]{4})([0-9]{1})([0-9]{3})|NGN1([0-9]{4})([0-9]{1})([0-9]{3})([0]{2})([0-9]{2})|[0-9]{10})$", message = "Debit account must be either 10 Digit Customer Account or NGN account number")
    @Schema(name = "Debit Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String debitAccount;

    @NotBlank(message = "Credit account is required")
    @Pattern(regexp = "^(PL([5-6])([0-9]{4}))|(NGN1([0-9]{4})([0-9]{1})([0-9]{3})|NGN1([0-9]{4})([0-9]{1})([0-9]{3})([0]{2})([0-9]{2})|[0-9]{10})$", message = "Credit account must be either PL or NGN account number")
    @Schema(name = "Credit Account Number", example = "0123456789", description = "PL Account or Internal Account")
    private String creditAccount;

    @NotBlank(message = "Transaction narration is required")
    @Schema(name = "Transaction Narration", example = "Cash Withdrawal IFO Brian Okon", description = "Transaction Narration")
    private String narration;

    @NotBlank(message = "Transaction amount is required")
    @Pattern(regexp = "(?=.*?\\d)^\\$?(([1-9]\\d{0,2}(,\\d{3})*)|\\d+)?(\\.\\d{1,3})?$", message = "Transaction Amount must contain only digits, comma or dot only")
    @Schema(name = "Transaction Amount", example = "1,000.00", description = "Transaction Amount")
    private String amount;

    @NotNull(message = "Transaction type cannot be null")
    @NotEmpty(message = "Transaction type cannot be empty")
    @NotBlank(message = "Transaction type cannot be blank")
    @Schema(name = "Transaction type", example = "ACTF", description = "Transaction Type")
    private String transType;

    @Schema(name = "Inputter", example = "BOKON", description = "Inputter")
    private String inputter;
    @Schema(name = "Authorizer", example = "BOKON", description = "Authorizer")
    private String authorizer;

    @NotNull(message = "Number of authorizer cannot be null")
    @NotEmpty(message = "Number of authorizer cannot be empty")
    @NotBlank(message = "Number of authorizer cannot be blank")
    @Schema(name = "Number of Authorizer", example = "0", description = "Number of Authorizer")
    @Pattern(regexp = "[0-2]{1}", message = "No of Authorizer must be between 0-2")
    private String noOfAuthorizer;

    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "PYLON67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;

    @NotBlank(message = "Request ID is required")
    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Request ID is required")
    private String requestId;

    @Valid
    private List<ChargeInfo> chargeTypes;

    @NotNull(message = "Token cannot be null")
    @NotEmpty(message = "Token cannot be empty")
    @NotBlank(message = "Token cannot be blank")
    private String token;

    private boolean useCommissionAsChargeType = false;
}
