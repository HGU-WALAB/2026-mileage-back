package com.csee.swplus.mileage.subitem.mapper;

import com.csee.swplus.mileage.subitem.dto.DetailResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DetailMapper {
    List<DetailResponseDto> findAllDetailSubitems(@Param("studentId") String studentId, @Param("currentSemester") String currentSemester);
}

