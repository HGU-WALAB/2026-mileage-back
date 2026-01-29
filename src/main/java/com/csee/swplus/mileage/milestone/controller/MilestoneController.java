package com.csee.swplus.mileage.milestone.controller;

import com.csee.swplus.mileage.milestone.dto.response.MilestonePointResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneSemesterTotalPointResponseDto;
import com.csee.swplus.mileage.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // 이 class 가 REST API 관련 class 라는 것을 스프링에게 명시
@RequestMapping("/api/mileage/capability")
@RequiredArgsConstructor
@Slf4j
public class MilestoneController {
    public final MilestoneService milestoneService;

    @GetMapping("")
    public ResponseEntity<List<MilestoneResponseDto>> getMilestoneCapabilities() {
        return ResponseEntity.ok(
                milestoneService.getMilestoneCapabilities()
        );
    }

    @GetMapping("/milestone")
    public ResponseEntity<List<MilestonePointResponseDto>> getMilestonePoint() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        int studentId = Integer.parseInt(currentUserId);
        return ResponseEntity.ok(
                milestoneService.getMilestonePoint(studentId)
        );
    }

    @GetMapping("/semester")
    public ResponseEntity<List<MilestoneSemesterTotalPointResponseDto>> getTotalMilestoneSemester() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        int studentId = Integer.parseInt(currentUserId);
        return ResponseEntity.ok(
                milestoneService.getTotalMilestoneSemester(studentId)
        );
    }

//    @GetMapping("/milestone/{studentId}")
//    public ResponseEntity<List<MilestoneSemesterTotalPointResponseDto>> getMilestoneSemester(
//            @PathVariable int studentId
//    ) {
//        return ResponseEntity.ok(
//                milestoneService.getMilestoneSemester(studentId)
//        );
//    }
}
