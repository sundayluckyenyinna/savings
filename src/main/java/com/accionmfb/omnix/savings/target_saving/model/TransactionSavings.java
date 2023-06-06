package com.accionmfb.omnix.savings.target_saving.model;

import com.accionmfb.omnix.savings.target_saving.constant.ModelStatus;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_savings")
@Getter
@Setter
public class TransactionSavings
{
    @Id
    private Long id;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "transaction_request_id")
    private String transactionRequestId;

    @Column(name = "transaction_executed_date")
    private LocalDateTime savingExecutedDate;

    @Column(name = "tran_ref")
    private String transactionRef;

    @Column(name = "tran_t24_ref")
    private String transactionT24Ref;

    @Column(name = "saving_t24_ref")
    private String savingT24Ref;

    @Column(name = "status")
    private String status = ModelStatus.PENDING.name();

    @Column(name = "debit_account")
    private String debitAccount;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "transaction_amount")
    private String transactionAmount;

    @Column(name = "failure_reason")
    private String failureReason;
}
