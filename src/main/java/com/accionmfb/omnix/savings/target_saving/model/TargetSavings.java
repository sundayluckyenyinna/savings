package com.accionmfb.omnix.savings.target_saving.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "target_savings")
public class TargetSavings
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "goal_name")
    private String goalName;

    @Column(name = "target_amount")
    private String targetAmount;

    @Column(name = "tenor_in_month")
    private String tenorInMonth;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "savings_amount")
    private String savingsAmount;

    @Column(name = "interest_rate")
    private String interestRate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "earliest_termination_date")
    private LocalDate earliestTerminationDate;

    @Column(name = "terminated_by")
    private String terminatedBy;

    @Column(name = "milestone_amount")
    private String milestoneAmount;

    @Column(name = "milestone_percent")
    private String milestonePercent;

    @Column(name = "time_period")
    private char timePeriod;

    @Column(name = "status")
    private String status;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "interest_accrued")
    private String interestAccrued;

    @Column(name = "interest_paid_at")
    private LocalDate interestPaidAt;

    @Column(name = "interest_paid")
    private boolean interestPaid = false;

    @Column(name = "interest_failure_reason")
    private String interestFailureReason;

    @Column(name = "trans_ref")
    private String transRef;

    @Column(name = "sms_25_percent_send")
    private boolean sms25PercentSend = false;

    @Column(name = "sms_50_percent_send")
    private boolean sms50PercentSend = false;

    @Column(name = "sms_75_percent_send")
    private boolean sms75PercentSend = false;

    @Column(name = "sms_100_percent_send")
    private boolean sms100PercentSend = false;

    @Column(name = "total_interest")
    private String totalInterest;

    @Column(name = "daily_interest")
    private String dailyInterest;

    @Column(name = "contribution_count_for_month")
    private int contributionCountForMonth = 0;

    @Override
    public String toString() {
        return "TargetSavings{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", accountNumber='" + accountNumber + '\'' +
                ", goalName='" + goalName + '\'' +
                ", targetAmount='" + targetAmount + '\'' +
                ", tenorInMonth='" + tenorInMonth + '\'' +
                ", frequency='" + frequency + '\'' +
                ", savingsAmount='" + savingsAmount + '\'' +
                ", interestRate='" + interestRate + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", terminationDate=" + terminationDate +
                ", earliestTerminationDate=" + earliestTerminationDate +
                ", terminatedBy='" + terminatedBy + '\'' +
                ", milestoneAmount='" + milestoneAmount + '\'' +
                ", milestonePercent='" + milestonePercent + '\'' +
                ", timePeriod=" + timePeriod +
                ", status='" + status + '\'' +
                ", requestId='" + requestId + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", interestAccrued='" + interestAccrued + '\'' +
                ", interestPaidAt=" + interestPaidAt +
                ", interestPaid=" + interestPaid +
                ", interestFailureReason='" + interestFailureReason + '\'' +
                ", transRef='" + transRef + '\'' +
                ", sms25PercentSend=" + sms25PercentSend +
                ", sms50PercentSend=" + sms50PercentSend +
                ", sms75PercentSend=" + sms75PercentSend +
                ", sms100PercentSend=" + sms100PercentSend +
                ", totalInterest='" + totalInterest + '\'' +
                ", dailyInterest='" + dailyInterest + '\'' +
                ", contributionCountForMonth=" + contributionCountForMonth +
                '}';
    }
}
