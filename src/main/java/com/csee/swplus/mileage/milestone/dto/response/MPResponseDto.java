package com.csee.swplus.mileage.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MPResponseDto {
    private int capabilityId;
    private String capabilityName;
    private int totalMilestoneCount; // 필터링된 학생들의 총 마일스톤 수
    private int groupSize;// 필터링된 학생 수
    private int averageMilestoneCount;
}

