package com.csee.swplus.mileage.etcSubitem.mapper;  // service와 같은 레벨의 mapper 패키지

import com.csee.swplus.mileage.etcSubitem.dto.StudentInputSubitemResponseDto;
import com.csee.swplus.mileage.etcSubitem.dto.EtcSubitemResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EtcSubitemMapper {
    List<StudentInputSubitemResponseDto> findAllStudentInputSubitems(@Param("currentSemester") String currentSemester);

    List<EtcSubitemResponseDto> findAllEtcSubitems(@Param("studentId") String studentId, @Param("currentSemester") String currentSemester);

    int getMPoint(@Param("subitemId") int subitemId);

    String getSname(@Param("studentId") String studentId);
}
