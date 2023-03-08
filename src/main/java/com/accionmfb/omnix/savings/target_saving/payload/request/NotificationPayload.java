package com.accionmfb.omnix.savings.target_saving.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload
{
    private String amount;
    private String accountNumber;
    private String branch;
    private String transDate;
    private String transTime;
    private String narration;
    private String accountBalance;
    private String mobileNumber;
    private String requestId;
    private String token;
    private String accountType;
    private String lastName;
    private String otherName;
    private String email;
    private String emailType;
    private String startDate;
    private String endDate;
    private String emailSubject;
    private char smsType;
    private String smsFor;
    private String otp;
    private String targetAmount;
    private String currentAmount;
    private String milestonePercent;
}
