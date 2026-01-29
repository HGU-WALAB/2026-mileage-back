package com.csee.swplus.mileage.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestItemResponseDto {
    private String capabilityName;
    private List<String> suggestion;
}

