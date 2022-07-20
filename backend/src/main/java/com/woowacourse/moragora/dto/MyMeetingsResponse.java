package com.woowacourse.moragora.dto;

import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.service.closingstrategy.TimeChecker;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class MyMeetingsResponse {

    private final LocalTime serverTime;
    private final List<MyMeetingResponse> meetings;

    private MyMeetingsResponse(final LocalTime serverTime, final List<MyMeetingResponse> meetings) {
        this.serverTime = serverTime;
        this.meetings = meetings;
    }

    public static MyMeetingsResponse of(final LocalTime now,
                                        final TimeChecker timeChecker,
                                        final List<Meeting> meetings) {
        final List<MyMeetingResponse> myMeetingResponses = meetings.stream()
                .map(meeting -> MyMeetingResponse.of(now, timeChecker, meeting))
                .collect(Collectors.toList());

        return new MyMeetingsResponse(now, myMeetingResponses);
    }
}
