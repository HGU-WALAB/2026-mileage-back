package com.csee.swplus.mileage.auth.controller.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString  // 로그 출력 시 필드 값들을 확인할 수 있도록
public class PublicLoginResponse {
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String department;
    private String major1;
    private String major2;
    private Integer grade;
    private Integer term;
    private String currentSemester;
    private String studentType;

    public static PublicLoginResponse from(LoginResponse loginResponse) {
        return PublicLoginResponse.builder()
                .studentId(loginResponse.getStudentId())
                .studentName(loginResponse.getStudentName())
                .studentEmail(loginResponse.getStudentEmail())
                .department(loginResponse.getDepartment())
                .major1(loginResponse.getMajor1())
                .major2(loginResponse.getMajor2())
                .grade(loginResponse.getGrade())
                .term(loginResponse.getTerm())
                .currentSemester(loginResponse.getCurrentSemester())
                .studentType(loginResponse.getStudentType())
                .build();
    }
}
