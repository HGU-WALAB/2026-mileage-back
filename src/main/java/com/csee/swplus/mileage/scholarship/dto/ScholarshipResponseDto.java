package com.csee.swplus.mileage.scholarship.dto;

import com.csee.swplus.mileage.user.controller.response.UserResponse;
import com.csee.swplus.mileage.user.entity.Users;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScholarshipResponseDto {
    private Integer isApply;  // Changed to camelCase

    public static ScholarshipResponseDto from(Users user) {
        return ScholarshipResponseDto.builder()
                .isApply(user.getIsApply())
                .build();
    }
}