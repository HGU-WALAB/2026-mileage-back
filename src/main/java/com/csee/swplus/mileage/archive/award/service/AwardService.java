package com.csee.swplus.mileage.archive.award.service;

import com.csee.swplus.mileage.archive.award.dto.AwardResponseDto;
import com.csee.swplus.mileage.archive.award.mapper.AwardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwardService {
    private final AwardMapper awardMapper;

    /**
     * 수상 내역 목록 조회 (현재 로그인한 사용자 기준)
     */
    public List<AwardResponseDto> getAwards(String studentId) {
        log.info("Fetching awards for studentId={}", studentId);
        return awardMapper.findAllAwards(studentId);
    }
}

