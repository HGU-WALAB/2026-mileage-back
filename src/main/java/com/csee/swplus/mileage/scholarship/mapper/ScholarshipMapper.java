package com.csee.swplus.mileage.scholarship.mapper;  // service와 같은 레벨의 mapper 패키지

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ScholarshipMapper {
    int updateStudentApplicationStatus(@Param("studentId") String studentId,
                          @Param("applyDate") LocalDateTime applyDate,
                          @Param("isChecked") int isChecked);

    int createApplication(@Param("studentId") String studentId,
                          @Param("isChecked") int isChecked,
                          @Param("semester") String semester);
}
