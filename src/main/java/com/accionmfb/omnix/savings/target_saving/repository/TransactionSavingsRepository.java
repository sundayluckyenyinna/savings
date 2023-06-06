package com.accionmfb.omnix.savings.target_saving.repository;

import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavings;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionSavingsRepository
{
    List<TransactionSavings> getPendingTransactionSavings();
    TransactionSavings updateTransactionSavings(TransactionSavings transactionSavings);
    TransactionSavingSetup getTransactionSavingSetupByAccountAndType(String accountNumber, String type);

    TransactionSavingSetup saveTransactionSavingSetup(TransactionSavingSetup transactionSavingSetup);
    TransactionSavingSetup updatedTransactionSavingSetup(TransactionSavingSetup transactionSavingSetup);
    List<TransactionSavingSetup> getTransactionSavingSetupByAccount(String accountNumber);
}
