package com.csee.swplus.mileage.subitem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubitemRequestDto {
    private String studentId;
    private String keyword;
    private String category;
    private String semester;
    private String done;
}