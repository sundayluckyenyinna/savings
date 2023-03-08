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
@Table(name = "target_savings_schedule")
public class TargetSavingSchedule
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "due_at")
    private LocalDate dueAt;

    @Column(name = "executed_at")
    private LocalDate executedAt;

    @Column(name = "debit_account")
    private String debitAccount;

    @Column(name = "credit_account")
    private String creditAccount;

    @Column(name = "t24_trans_ref")
    private String t24TransRef;

    @Column(name = "amount")
    private String amount;

    @Column(name = "sms_due_date")
    private LocalDate smsDueDate;

    @Column(name = "sms_due_send")
    private boolean smsDueSend = false;

    @ManyToOne(cascade = CascadeType.ALL)
    private TargetSavings targetSavings;

    @Column(name = "time_period")
    private char timePeriod;

    @Column(name = "status")
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

}
