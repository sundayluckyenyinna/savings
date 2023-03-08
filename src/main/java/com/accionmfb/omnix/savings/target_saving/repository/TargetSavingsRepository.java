package com.accionmfb.omnix.savings.target_saving.repository;

import com.accionmfb.omnix.savings.target_saving.constant.TargetSavingStatus;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavingSchedule;
import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetSavingsRepository
{
    Optional<TargetSavings> findTargetSavingsByRequestId(String requestId);

    List<TargetSavings> findAllTargetSavingsByAccountNumber(String accountNumber);

    Optional<TargetSavings> findTargetSavingsByGoalNameAndAccountNumber(String goalName, String accountNumber);

    Optional<TargetSavings> findTargetSavingsById(Long id);
    List<TargetSavings> findAllTargetSavings();

    void removeTargetSavings(TargetSavings targetSavings);

    TargetSavingSchedule saveTargetSavingSchedule(TargetSavingSchedule targetSavingSchedule);

    TargetSavings saveTargetSavings(TargetSavings targetSavings);

    TargetSavings updateTargetSavings(TargetSavings targetSavings);

    TargetSavingSchedule updateTargetSavingSchedule(TargetSavingSchedule targetSavingSchedule);

    List<TargetSavings> findAllTargetSavingsNotTerminated();

    List<TargetSavingSchedule> findAllTargetSavingSchedulesByParent(TargetSavings targetSavings);

    void removeTargetSavingSchedule(TargetSavings targetSavings);

    void revertFromTargetSavingsSetup(TargetSavings targetSavings);

    List<TargetSavingSchedule> findAllMissedSchedulesOfTargetSavings(TargetSavings targetSavings);

    List<TargetSavingSchedule> findAllPendingSchedulesOfTargetSavings(TargetSavings targetSavings);

    List<TargetSavingSchedule> findAllSchedulesOfTargetSavingsByStatus
            (TargetSavings targetSavings, TargetSavingStatus status);

    List<TargetSavingSchedule> findAllPendingAndFailedSchedulesByParent(TargetSavings targetSavings);
    int findCountOfMissedTargetSavingScheduleOfTargetSavings(TargetSavings targetSavings);

}
