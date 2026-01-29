package com.csee.swplus.mileage.scholarship.dto;

import lombok.Getter;
import lombok.Setter;

// getter/setter annotation 으로 설정 시 이름 문제 발생
public class ScholarshipRequestDto {
    private boolean isAgree;

    public boolean getIsAgree() {
        return isAgree;
    }

    public void setIsAgree(boolean isAgree) {
        this.isAgree = isAgree;
    }
}