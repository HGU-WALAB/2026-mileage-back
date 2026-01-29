package com.csee.swplus.mileage.subitem.service;

import com.csee.swplus.mileage.subitem.dto.SubitemRequestDto;
import com.csee.swplus.mileage.subitem.dto.SubitemResponseDto;
import com.csee.swplus.mileage.subitem.mapper.SubitemMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubitemService {
    private final SubitemMapper subitemMapper;

    public List<SubitemResponseDto> getSubitems(SubitemRequestDto requestDto) {
        log.info("Searching subitems with keyword: '{}'", requestDto.getKeyword());
        return subitemMapper.findSubitems(
                requestDto.getStudentId(),
                requestDto.getKeyword(),
                requestDto.getCategory(),
                requestDto.getSemester(),
                requestDto.getDone()
        );
    }
}
