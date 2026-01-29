package com.csee.swplus.mileage.user.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "_sw_mileage_student_school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSchool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school", length = 40)
    private String school;

    @Column(name = "major_1", length = 60)
    private String major1;

    @Column(name = "major_2", length = 60)
    private String major2;

    private String stype;

    public static StudentSchool from(Users user) {
        return StudentSchool.builder()
                .school(user.getDepartment())
                .major1(user.getMajor1())
                .major2(user.getMajor2())
                .build();
    }
}
