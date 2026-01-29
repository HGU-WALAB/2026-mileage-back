package com.csee.swplus.mileage.milestone.service;

import com.csee.swplus.mileage.milestone.dto.response.MilestonePointResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneSemesterResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneSemesterTotalPointResponseDto;
import com.csee.swplus.mileage.milestone.mapper.MilestoneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {
    private final MilestoneMapper milestoneMapper;

    public List<MilestoneResponseDto> getMilestoneCapabilities() {
        List<MilestoneResponseDto> res = milestoneMapper.findAllMilestoneCapability();
        log.info("📝 findAllMilestoneCapability 결과 - res: {}", res);
        return res;
    }

    public List<MilestonePointResponseDto> getMilestonePoint(int studentId) {
        List<MilestonePointResponseDto> res = milestoneMapper.findAllMilestonePoint(studentId);
        log.info("📝 findAllMilestonePoint 결과 - res: {}", res);
        return res;
    }

//    public List<MilestoneSemesterResponseDto> getMilestoneSemester(int studentId) {
//        List<MilestoneSemesterResponseDto> res = milestoneMapper.findEachMilestoneBySemester(studentId);
//        log.info("📝 findEachMilestoneBySemester 결과 - res: {}", res);
//        return res;
//    }

    public List<MilestoneSemesterTotalPointResponseDto> getTotalMilestoneSemester(int studentId) {
        List<MilestoneSemesterTotalPointResponseDto> res = milestoneMapper.findAllMilestoneBySemester(studentId);
        log.info("📝 findAllMilestoneBySemester 결과 - res: {}", res);
        return res;
    }
}
