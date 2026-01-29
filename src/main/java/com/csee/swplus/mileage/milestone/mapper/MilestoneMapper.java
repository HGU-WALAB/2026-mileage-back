package com.csee.swplus.mileage.milestone.mapper;

import com.csee.swplus.mileage.milestone.dto.response.MilestonePointResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneSemesterResponseDto;
import com.csee.swplus.mileage.milestone.dto.response.MilestoneSemesterTotalPointResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MilestoneMapper {
    List<MilestoneResponseDto> findAllMilestoneCapability();

    List<MilestonePointResponseDto> findAllMilestonePoint(@Param("studentId") int studentId);

//    List<MilestoneSemesterResponseDto> findEachMilestoneBySemester(@Param("studentId") int studentId);

    List<MilestoneSemesterTotalPointResponseDto> findAllMilestoneBySemester(@Param("studentId") int studentId);
}
