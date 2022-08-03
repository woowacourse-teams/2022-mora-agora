package com.woowacourse.auth.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(exclude = "accessToken")
public class LoginResponse {

    private final String accessToken;

    public LoginResponse(final String accessToken) {
        this.accessToken = accessToken;
    }
}
