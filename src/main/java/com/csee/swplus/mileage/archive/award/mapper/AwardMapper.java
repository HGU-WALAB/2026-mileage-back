package com.csee.swplus.mileage.archive.award.mapper;

import com.csee.swplus.mileage.archive.award.dto.AwardResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AwardMapper {
    List<AwardResponseDto> findAllAwards(@Param("studentId") String studentId);
}

