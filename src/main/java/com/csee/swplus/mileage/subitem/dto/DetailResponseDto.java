package com.csee.swplus.mileage.subitem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetailResponseDto {
    private Long subitemId;
    private String subitemName;

    private Long capabilityId;      // ← _sw_milestone.id
    private String capabilityName;  // ← _sw_milestone.name

    private String semester;

    private Long recordId;
    private String description1;

    private Boolean done;
}

