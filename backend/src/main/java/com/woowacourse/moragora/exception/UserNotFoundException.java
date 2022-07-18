package com.woowacourse.moragora.exception;

public class UserNotFoundException extends RuntimeException {

    private static final String MESSAGE = "유저가 존재하지 않습니다.";

    public UserNotFoundException() {
        super(MESSAGE);
    }
}
