package com.csee.swplus.mileage.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneSemesterTotalPointResponseDto {
    private String semester;            // 특정 학기
    private int userMilestoneCount;     // 학생이 해당 학기에 쌓은 마일스톤 역량 개수
    private int totalMilestoneCount;   // 해당 학기에 쌓을 수 있는 마일스톤 역량 total 개수
}
