package com.csee.swplus.mileage.auth.dto;

import com.csee.swplus.mileage.auth.controller.request.LoginRequest;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthDto {
    private String studentId;
    private String hisnetToken;
    private String token;
    private String studentName;
    private String studentEmail;
    private String department;
    private String major1;
    private String major2;
    private Integer grade;
    private Integer term;
    private String currentSemester;
    private String studentType;

    // 기존에 있던 메서드와 다른 형태로 변경
    public static AuthDto from(LoginRequest request) {
        return AuthDto.builder()
                .hisnetToken(request.getToken())  // `LoginRequest`에서 가져오는 필드만 사용
                .build();
    }

    // toString() 메서드 오버라이드
    @Override
    public String toString() {
        return "AuthDto{" +
                "studentId='" + studentId + '\'' +
                ", hisnetToken='" + hisnetToken + '\'' +
                ", token='" + token + '\'' +
                ", studentName='" + studentName + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                ", department='" + department + '\'' +
                ", major1='" + major1 + '\'' +
                ", major2='" + major2 + '\'' +
                ", grade=" + grade +
                ", term=" + term +
                ", currentSemester=" + currentSemester +
                '}';
    }
}
