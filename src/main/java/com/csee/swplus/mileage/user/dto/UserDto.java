package com.csee.swplus.mileage.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String department;
    private String major1;
    private String major2;
    private Integer grade;
    private Integer term;
    private String isStudent;
    private LocalDateTime modDate;
}
