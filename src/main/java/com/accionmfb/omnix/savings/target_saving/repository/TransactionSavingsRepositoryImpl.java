package com.accionmfb.omnix.savings.target_saving.repository;

import com.accionmfb.omnix.savings.target_saving.constant.ModelStatus;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavingSetup;
import com.accionmfb.omnix.savings.target_saving.model.TransactionSavings;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public class TransactionSavingsRepositoryImpl implements TransactionSavingsRepository
{

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<TransactionSavings> getPendingTransactionSavings() {
        TypedQuery<TransactionSavings> query = em.createQuery("Select t from TransactionSavings t where t.status = :status", TransactionSavings.class)
                .setParameter("status", ModelStatus.PENDING.name());
        return query.getResultList();
    }

    @Override
    public TransactionSavings updateTransactionSavings(TransactionSavings transactionSavings) {
        em.merge(transactionSavings);
        em.flush();
        return transactionSavings;
    }

    @Override
    public TransactionSavingSetup getTransactionSavingSetupByAccountAndType(String accountNumber, String type) {
        TypedQuery<TransactionSavingSetup> query = em.createQuery("Select tss from TransactionSavingSetup tss where tss.accountNumber = :acN and tss.transactionType = :transType", TransactionSavingSetup.class)
                .setParameter("acN", accountNumber)
                .setParameter("transType", type);
        return query.getResultList().stream().findFirst().orElse(null);
    }

    @Override
    public TransactionSavingSetup saveTransactionSavingSetup(TransactionSavingSetup transactionSavingSetup){
        em.persist(transactionSavingSetup);
        em.flush();
        return transactionSavingSetup;
    }

    @Override
    public TransactionSavingSetup updatedTransactionSavingSetup(TransactionSavingSetup transactionSavingSetup) {
        em.merge(transactionSavingSetup);
        em.flush();
        return transactionSavingSetup;
    }

    @Override
    public List<TransactionSavingSetup> getTransactionSavingSetupByAccount(String accountNumber) {
        TypedQuery<TransactionSavingSetup> query = em.createQuery("Select tss from TransactionSavingSetup tss where tss.accountNumber = :acN", TransactionSavingSetup.class)
                .setParameter("acN", accountNumber);
        return query.getResultList();
    }
}
