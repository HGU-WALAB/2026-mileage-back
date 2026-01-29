package com.csee.swplus.mileage.user.controller.response;

import lombok.*;
import com.csee.swplus.mileage.user.entity.Users;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String department;
    private String major1;
    private String major2;
    private Integer grade;
    private Integer term;
    private String studentType;
    private LocalDateTime modDate;

    public static UserResponse from(Users user, String stype) {
        return UserResponse.builder()
                .studentId(user.getUniqueId())
                .studentName(user.getName())
                .studentEmail(user.getEmail())
                .department(user.getDepartment())
                .major1(user.getMajor1())
                .major2(user.getMajor2())
                .grade(user.getGrade())
                .term(user.getSemester())
                .studentType(stype)
                .modDate(user.getModdate())
                .build();
    }
}
