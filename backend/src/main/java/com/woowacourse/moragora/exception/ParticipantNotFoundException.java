package com.woowacourse.moragora.exception;

public class ParticipantNotFoundException extends RuntimeException {

    private static final String MESSAGE = "해당 미팅에 유저가 존재하지 않습니다.";

    public ParticipantNotFoundException() {
        super(MESSAGE);
    }
}
