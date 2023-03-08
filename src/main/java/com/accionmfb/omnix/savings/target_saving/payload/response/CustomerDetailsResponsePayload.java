package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.Data;

@Data
public class CustomerDetailsResponsePayload
{
    private String responseCode;
    private String responseMessage;
    private String customerNumber;
    private String lastName;
    private String otherName;
    private String mnemonic;
    private String maritalStatus;
    private String gender;
    private String dob;
    private String branchCode;
    private String branchName;
    private String mobileNumber;
    private String stateOfResidence;
    private String cityOfResidence;
    private String residentialAddress;
    private String kyc;
    private String status;
    private String primaryAccount;
    private String securityQuestion;
    private String customerType;
    private boolean boarded;
    private String bVN;
    private String referalCode;
    private String issueDate;
    private String expiryDate;
    private String accountNumber;
    private String cardStatus;
    private String allowedTerm;
}
