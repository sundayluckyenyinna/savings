package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.Data;

@Data
public class AccountDetailsResponsePayload
{
    private String responseMessage;
    private String accountName;
    private String accountNumber;
    private boolean wallet;
    private String productCode;
    private String productName;
    private String category;
    private String branch;
    private String branchCode;
    private boolean openedWithBVN;
    private String customerNumber;
    private String responseCode;
    private String mobileNumber;
    private String status;
}
