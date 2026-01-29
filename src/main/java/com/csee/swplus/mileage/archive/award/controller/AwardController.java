package com.csee.swplus.mileage.archive.award.controller;

import com.csee.swplus.mileage.archive.award.dto.AwardResponseDto;
import com.csee.swplus.mileage.archive.award.service.AwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mileage/award")
@RequiredArgsConstructor
@Slf4j
public class AwardController {

    private final AwardService awardService;

    /**
     * GET /api/mileage/award
     * 수상 내역 목록 조회 (Ver2 패턴과 동일, 현재 로그인한 사용자 기준)
     */
    @GetMapping("")
    public ResponseEntity<List<AwardResponseDto>> getAllAwards() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Fetching award list for studentId={}", currentUserId);
        return ResponseEntity.ok(
                awardService.getAwards(currentUserId)
        );
    }
}

