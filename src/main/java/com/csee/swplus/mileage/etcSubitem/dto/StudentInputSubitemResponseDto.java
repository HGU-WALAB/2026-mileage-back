package com.csee.swplus.mileage.etcSubitem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class StudentInputSubitemResponseDto {    // 관리자가 신청 받기를 허용한 (subitem 테이블의 student_input = 'Y') 기타 항목 (subitem) 전용 DTO
    private int subitemId;
    private String subitemName;
    private int categoryId;
    private String categoryName;
    private String semester;
}

