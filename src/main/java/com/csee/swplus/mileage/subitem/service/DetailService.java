package com.csee.swplus.mileage.subitem.service;

import com.csee.swplus.mileage.setting.service.ManagerService;
import com.csee.swplus.mileage.subitem.dto.DetailResponseDto;
import com.csee.swplus.mileage.subitem.mapper.DetailMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetailService {
    private final DetailMapper detailMapper;
    private final ManagerService managerService;

    public List<DetailResponseDto> getAllDetailSubitems(String studentId) {
        log.info("Fetching all detail subitems");
        String currentSemester = managerService.getCurrentSemester();
        return detailMapper.findAllDetailSubitems(studentId, currentSemester);
    }
}

