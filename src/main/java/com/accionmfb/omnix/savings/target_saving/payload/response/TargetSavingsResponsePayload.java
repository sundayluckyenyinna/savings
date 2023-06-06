/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.savings.target_saving.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetSavingsResponsePayload {

    private String id;
    private String goalName;
    private String targetAmount;
    private String startDate;
    private String endDate;
    private String contributionFrequency;
    private String savingsAmount;
    private String interestRate;
    private String responseCode;
    private String earliestTerminationDate;
    private String tenorInMonths;
    private String status;
    private String terminatedBy;
    private String dateTerminated;
    private String milestoneAmount;
    private String milestonePercentage;
    private String accountNumber;
    private String customerName;
    private String mobileNumber;
    private String dueAt;
    private String refId;
    private String totalMissedAmount;
    private String responseMessage;
    private String interest;
}
