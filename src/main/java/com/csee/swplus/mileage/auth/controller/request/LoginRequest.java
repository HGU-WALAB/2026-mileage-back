package com.csee.swplus.mileage.auth.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LoginRequest {
    private String accessKey;
    private String token;
}