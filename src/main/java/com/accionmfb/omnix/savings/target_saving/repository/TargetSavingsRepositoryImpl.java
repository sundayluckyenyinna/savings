package com.accionmfb.omnix.savings.target_saving.repository;

import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus.*;

@Repository
@Transactional
public class TargetSavingsRepositoryImpl implements TargetSavingsRepository
{
    @PersistenceContext
    EntityManager em;


    /** OPERATIONS RELATING TO TARGET SAVING */
    @Override
    public Optional<TargetSavings> findTargetSavingsByRequestId(String requestId) {
        TypedQuery<TargetSavings> query =
                em.createQuery("SELECT t FROM TargetSavings t WHERE t.requestId = :requestId", TargetSavings.class)
                        .setParameter("requestId", requestId);
        List<TargetSavings> targetSavingsList = query.getResultList();
        if( targetSavingsList.isEmpty())
            return Optional.empty();
        return targetSavingsList.stream().findFirst();
    }

    @Override
    public List<TargetSavings> findAllTargetSavingsByAccountNumber(String accountNumber){
        TypedQuery<TargetSavings> query =
                em.createQuery("SELECT t FROM TargetSavings t WHERE t.accountNumber = :accountNumber", TargetSavings.class)
                        .setParameter("accountNumber", accountNumber);
        List<TargetSavings> targetSavingsList = query.getResultList();

        return targetSavingsList;
    }

    @Override
    public Optional<TargetSavings> findTargetSavingsByGoalNameAndAccountNumber(String goalName, String accountNumber)
    {
        TypedQuery<TargetSavings> query =
                em.createQuery("SELECT t FROM TargetSavings t WHERE t.goalName = :goalName AND t.accountNumber = :accountNumber", TargetSavings.class)
                        .setParameter("goalName", goalName)
                        .setParameter("accountNumber", accountNumber);

        List<TargetSavings> targetSavingsList = query.getResultList();
        if( targetSavingsList.isEmpty())
            return Optional.ofNullable(null);
        return targetSavingsList.stream().findFirst();
    }

    @Override
    public List<TargetSavings> findAllTargetSavingsNotTerminated()
    {
        TypedQuery<TargetSavings> query = em
                .createQuery("SELECT t FROM TargetSavings t WHERE NOT t.status = :status",
                        TargetSavings.class)
                .setParameter("status", TERMINATED.name());

        return query.getResultList();
    }

    @Override
    public Optional<TargetSavings> findTargetSavingsById(Long id){
        TypedQuery<TargetSavings> query =
                em.createQuery("SELECT t FROM TargetSavings t WHERE t.id = :id", TargetSavings.class)
                        .setParameter("id", id);
        List<TargetSavings> targetSavingsList = query.getResultList();
        if(targetSavingsList.isEmpty())
            return Optional.empty();
        return targetSavingsList.stream().findFirst();
    }

    @Override
    public TargetSavings saveTargetSavings(TargetSavings targetSavings){
        em.persist(targetSavings);
        em.flush();
        return targetSavings;
    }

    @Override
    public TargetSavings updateTargetSavings(TargetSavings targetSavings) {
        em.merge(targetSavings);
        em.flush();
        return targetSavings;
    }

    @Override
    public List<TargetSavings> findAllTargetSavings()
    {
        TypedQuery<TargetSavings> query = em
                .createQuery("SELECT t FROM TargetSavings t", TargetSavings.class);

        return query.getResultList();
    }

    @Override
    public void removeTargetSavings(TargetSavings targetSavings)
    {
        Query query = em
                .createQuery("DELETE FROM TargetSavings t WHERE t.id = :id")
                .setParameter("id", targetSavings.getId());
        query.executeUpdate();
        em.flush();
    }

    /** OPERATIONS RELATING TO TARGET SAVING SCHEDULE */

    @Override
    public TargetSavingSchedule saveTargetSavingSchedule(TargetSavingSchedule targetSavingSchedule){
        em.persist(targetSavingSchedule);
        em.flush();
        return targetSavingSchedule;
    }

    @Override
    public TargetSavingSchedule updateTargetSavingSchedule(TargetSavingSchedule targetSavingSchedule) {
        em.merge(targetSavingSchedule);
        em.flush();
        return targetSavingSchedule;
    }

    @Override
    public List<TargetSavingSchedule> findAllTargetSavingSchedulesByParent(TargetSavings targetSavings)
    {
        TypedQuery<TargetSavingSchedule> query = em
                .createQuery(
                        "SELECT tss FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings",
                        TargetSavingSchedule.class
                )
                .setParameter("targetSavings", targetSavings);
        return query.getResultList();
    }

    @Override
    public void removeTargetSavingSchedule(TargetSavings targetSavings)
    {
        Query query = em
                .createQuery("DELETE FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings")
                .setParameter("targetSavings", targetSavings);
        query.executeUpdate();
        em.flush();
    }

    @Override
    public void revertFromTargetSavingsSetup(TargetSavings targetSavings)
    {
        removeTargetSavingSchedule(targetSavings);
        removeTargetSavings(targetSavings);
    }

    @Override
    public List<TargetSavingSchedule>
    findAllMissedSchedulesOfTargetSavings(TargetSavings targetSavings)
    {
        TypedQuery<TargetSavingSchedule> query =
                em.createQuery(
                        "SELECT tss FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings AND tss.status = :status", TargetSavingSchedule.class)
                        .setParameter("targetSavings", targetSavings)
                        .setParameter("status", "MISSED");
        List<TargetSavingSchedule> schedules = query.getResultList();
        return schedules;
    }

    @Override
    public List<TargetSavingSchedule>
    findAllPendingSchedulesOfTargetSavings(TargetSavings targetSavings)
    {
        TypedQuery<TargetSavingSchedule> query =
                em.createQuery(
                                "SELECT tss FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings AND tss.status = :status", TargetSavingSchedule.class)
                        .setParameter("targetSavings", targetSavings)
                        .setParameter("status", PENDING.name());

        return query.getResultList();
    }

    @Override
    public List<TargetSavingSchedule>
    findAllPendingAndFailedSchedulesByParent(TargetSavings targetSavings)
    {
        TypedQuery<TargetSavingSchedule> query =
                em.createQuery(
                                "SELECT tss FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings AND (tss.status = :status1 OR tss.status = :status2)", TargetSavingSchedule.class)
                        .setParameter("targetSavings", targetSavings)
                        .setParameter("status1", PENDING.name())
                        .setParameter("status2", FAILED.name());

        List<TargetSavingSchedule> schedules = query.getResultList();

        return schedules;
    }

    @Override
    public List<TargetSavingSchedule> findAllSchedulesOfTargetSavingsByStatus
            (TargetSavings targetSavings, TargetSavingStatus status)
    {
        TypedQuery<TargetSavingSchedule> query =
                em.createQuery(
                                "SELECT tss FROM TargetSavingSchedule tss WHERE tss.targetSavings = :targetSavings AND tss.status = :status", TargetSavingSchedule.class)
                        .setParameter("targetSavings", targetSavings)
                        .setParameter("status", status.name().toUpperCase());
        List<TargetSavingSchedule> schedules = query.getResultList();

        return schedules;
    }

    @Override
    public int findCountOfMissedTargetSavingScheduleOfTargetSavings(TargetSavings targetSavings)
    {
        return findAllMissedSchedulesOfTargetSavings(targetSavings).size();
    }

}
