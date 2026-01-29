package com.csee.swplus.mileage.etcSubitem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EtcSubitemResponseDto {   // 학생이 신청한 기타 항목 record 전용 DTO
    private int subitemId;
    private String subitemName;
    private String semester;
    private String description1;    // 등록 상세 정보
    private String description2;    // 추가 설명 (선택)
    private LocalDateTime regDate;  // 기타 항목 신청 일자
    private LocalDateTime modDate;  // 수정 일자
    private int recordId;
    private String file;            // 첨부 파일명
    private Integer fileId;             // 첨부 파일 PK
    private String uniqueFileName;  // 디비 및 uploads 디렉토리에 저장된 이름
}

