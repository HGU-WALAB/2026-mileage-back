package com.csee.swplus.mileage.user.service;

import com.csee.swplus.mileage.user.entity.StudentSchool;

public class UserServiceTest {
    public static void main(String[] args) {
        UserServiceTest test = new UserServiceTest();

        // 가짜 StudentSchool 객체 생성
        StudentSchool student1 = new StudentSchool(null, "전산전자공학부", "컴퓨터공학(33)", null, null);
        StudentSchool student3 = new StudentSchool(null, "경영학부", "경영(40)", "ACE(40)", null);

        // 테스트 실행 및 결과 출력
        System.out.println("학생1 결과: " + test.determineStype(student1)); // 예상: "전공"
        System.out.println("학생3 결과: " + test.determineStype(student3)); // 예상: "융합"
    }

    private String determineStype(StudentSchool studentSchool) {
        String school = studentSchool.getSchool();
        String major1 = studentSchool.getMajor1();
        String major2 = studentSchool.getMajor2();

        if ("전산전자공학부".equals(school)) {
            if ("AI·컴퓨터공학심화(60)".equals(major1) ||
                    "전자공학심화(60)".equals(major1) ||
                    "컴퓨터공학(33)".equals(major1) ||
                    "컴퓨터공학(40)".equals(major1) ||
                    "컴퓨터공학(45)".equals(major1) ||
                    "전자공학(33)".equals(major1)
            ) {}
            return "전공";
        }
        if ("글로벌리더십학부".equals(school)) {
            if ("AI·컴퓨터공학심화(60)".equals(major1)) {
                return "1학년";
            }
        }
        if ("컴퓨터공학(33)".equals(major2) ||
                "컴퓨터공학(40)".equals(major2) ||
                "컴퓨터공학(45)".equals(major2) ||
                "IT(40)".equals(major2) ||
                "ICT(45)".equals(major2) ||
                "ACE(40)".equals(major2) ||
                "DS(45)".equals(major2)
        ) {
            return "융합";
        }
        if ("IT(40)".equals(major1) ||
                "ICT(45)".equals(major1) ||
                "ACE(40)".equals(major1)
        ) {
            return "융합";
        }
        if (("생명공학(33)".equals(major1)) || ("경영(40)".equals(major1)) ||
                ("경제(45)".equals(major1))){
            if ("AI융합".equals(major2)) {
                return "융합";
            }
        }
        return "기타";
    }
}
