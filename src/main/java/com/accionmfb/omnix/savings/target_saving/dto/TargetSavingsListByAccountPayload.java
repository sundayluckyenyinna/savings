package com.accionmfb.omnix.savings.target_saving.dto;

import com.accionmfb.omnix.savings.target_saving.model.TargetSavings;
import lombok.Data;

import java.util.List;

@Data
public class TargetSavingsListByAccountPayload
{
    private String accountNumber;
    private List<TargetSavings> targetSavingsList;
}
