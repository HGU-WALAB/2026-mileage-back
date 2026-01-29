package com.csee.swplus.mileage.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MilestoneResponseDto {
    private int capabilityId;
    private String capabilityName;
    private String description;
}
