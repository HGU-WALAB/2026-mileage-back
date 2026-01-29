package com.csee.swplus.mileage.subitem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubitemResponseDto {
    private Integer subitemId;
    private String subitemName;
    private Integer categoryId;
    private String categoryName;
    private String semester;
    private Boolean done;
    private Integer recordId;
    private String description1;
}
