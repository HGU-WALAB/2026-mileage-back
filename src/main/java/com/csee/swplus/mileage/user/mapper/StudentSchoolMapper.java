package com.csee.swplus.mileage.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentSchoolMapper {
    String findStype(@Param("school") String school,
                     @Param("major1") String major1,
                     @Param("major2") String major2);
}