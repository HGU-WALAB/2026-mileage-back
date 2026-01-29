package com.csee.swplus.mileage.subitem.controller;

import com.csee.swplus.mileage.subitem.dto.DetailResponseDto;
import com.csee.swplus.mileage.subitem.dto.SubitemRequestDto;
import com.csee.swplus.mileage.subitem.dto.SubitemResponseDto;
import com.csee.swplus.mileage.subitem.service.DetailService;
import com.csee.swplus.mileage.subitem.service.SubitemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mileage")
@RequiredArgsConstructor
public class SubitemController {
    private final SubitemService subitemService;
    private final DetailService detailService;

    @GetMapping("/search")
    public List<SubitemResponseDto> getSubitems(
            @RequestParam(required = false) String keyword,
            @RequestParam String category,
            @RequestParam String semester,
            @RequestParam String done) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Received search request - studentId: {}, query: '{}', category: {}, semester: {}, done: {}",
                currentUserId, keyword, category, semester, done);
        SubitemRequestDto requestDto = new SubitemRequestDto();
        requestDto.setStudentId(currentUserId);
        requestDto.setKeyword(keyword);
        requestDto.setCategory(category);
        requestDto.setSemester(semester);
        requestDto.setDone(done);
        return subitemService.getSubitems(requestDto);
    }

    @GetMapping("/detail")
    public List<DetailResponseDto> getAllDetailSubitems() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Received detail request for all subitems");
        return detailService.getAllDetailSubitems(currentUserId);
    }
}