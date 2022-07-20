package com.woowacourse.moragora.dto;

import com.woowacourse.moragora.entity.Meeting;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Getter;

@Getter
public class MeetingResponse {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Long id;
    private final String name;
    private final long attendanceCount;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String entranceTime;
    private final String leaveTime;
    private final List<UserResponse> users;

    public MeetingResponse(final Long id,
                           final String name,
                           final long attendanceCount,
                           final LocalDate startDate,
                           final LocalDate endDate,
                           final LocalTime entranceTime,
                           final LocalTime leaveTime,
                           final List<UserResponse> usersResponse) {
        this.id = id;
        this.name = name;
        this.attendanceCount = attendanceCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.entranceTime = entranceTime.format(TIME_FORMATTER);
        this.leaveTime = leaveTime.format(TIME_FORMATTER);
        this.users = usersResponse;
    }

    public static MeetingResponse of(final Meeting meeting,
                                     final List<UserResponse> userResponses,
                                     final long meetingAttendanceCount) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getName(),
                meetingAttendanceCount,
                meeting.getStartDate(),
                meeting.getEndDate(),
                meeting.getEntranceTime(),
                meeting.getLeaveTime(),
                userResponses
        );
    }
}
