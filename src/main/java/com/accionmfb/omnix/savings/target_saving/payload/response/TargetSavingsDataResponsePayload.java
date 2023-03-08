package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetSavingsDataResponsePayload
{
    private List<TargetSavingsResponsePayload> data;
    private String responseCode;
}
