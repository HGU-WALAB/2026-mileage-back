package com.csee.swplus.mileage.archive.award.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AwardResponseDto {
    private int awardId;
    private int awardYear;
    private LocalDate awardDate;
    private String contestName;
    private String awardName;
    private String awardType;
    private String organization;
    private LocalDateTime regDate;
    private LocalDateTime modDate;
}

