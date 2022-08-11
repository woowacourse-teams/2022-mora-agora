package com.woowacourse.moragora.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.woowacourse.moragora.dto.AttendanceResponse;
import com.woowacourse.moragora.dto.AttendancesResponse;
import com.woowacourse.moragora.dto.CoffeeStatResponse;
import com.woowacourse.moragora.dto.CoffeeStatsResponse;
import com.woowacourse.moragora.dto.UserAttendanceRequest;
import com.woowacourse.moragora.exception.ClientRuntimeException;
import com.woowacourse.moragora.exception.meeting.MeetingNotFoundException;
import com.woowacourse.moragora.exception.meeting.NotCheckInTimeException;
import com.woowacourse.moragora.exception.participant.ParticipantNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

class AttendanceControllerTest extends ControllerTest {

    @DisplayName("사용자 출석여부를 반영한다.")
    @Test
    void markAttendance() throws Exception {
        // given
        final Long meetingId = 1L;
        final Long userId = 1L;
        final UserAttendanceRequest request = new UserAttendanceRequest(true);

        validateToken("1");

        // when
        final ResultActions resultActions = performPost(
                "/meetings/" + meetingId + "/users/" + userId + "/attendances/today", request);

        // then
        resultActions.andExpect(status().isNoContent())
                .andDo(document("meeting/enter-Attendance",
                        requestFields(
                                fieldWithPath("isPresent").type(JsonFieldType.BOOLEAN).description("true")
                        )
                ));
    }

    @DisplayName("출석부가 활성화되지 않았을 때 출석을 제출하면 예외가 발생한다.")
    @Test
    void markAttendance_throwsException_ifCheckInIsNotActive() throws Exception {
        // given
        final Long meetingId = 99L;
        final Long userId = 1L;
        final UserAttendanceRequest request = new UserAttendanceRequest(true);

        validateToken("1");

        doThrow(new NotCheckInTimeException())
                .when(attendanceService)
                .updateAttendance(anyLong(), anyLong(), any(UserAttendanceRequest.class));

        // when
        final ResultActions resultActions = performPost(
                "/meetings/" + meetingId + "/users/" + userId + "/attendances/today", request);

        //then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("출석 가능한 시간이 아닙니다."));
    }

    @DisplayName("모임의 유저별 사용될 커피 스택 개수를 조회한다.")
    @Test
    void showUserCoffeeStats() throws Exception {
        // given
        final Long meetingId = 1L;
        final CoffeeStatsResponse coffeeStatsResponse = new CoffeeStatsResponse(
                List.of(
                        new CoffeeStatResponse(1L, "썬", 3L),
                        new CoffeeStatResponse(3L, "필즈", 2L),
                        new CoffeeStatResponse(5L, "포키", 1L),
                        new CoffeeStatResponse(6L, "쿤", 1L)
                )
        );
        given(attendanceService.countUsableCoffeeStack(any(Long.class)))
                .willReturn(coffeeStatsResponse);

        // when
        final ResultActions resultActions = performGet("/meetings/" + meetingId + "/coffees/use");

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.userCoffeeStats[?(@.id=='1')].coffeeCount", contains(3)))
                .andExpect(jsonPath("$.userCoffeeStats[?(@.id=='3')].coffeeCount", contains(2)))
                .andExpect(jsonPath("$.userCoffeeStats[?(@.id=='5')].coffeeCount", contains(1)))
                .andExpect(jsonPath("$.userCoffeeStats[?(@.id=='6')].coffeeCount", contains(1)))
                .andDo(document("meeting/usable-coffee",
                        responseFields(
                                fieldWithPath("userCoffeeStats[].id").type(JsonFieldType.NUMBER)
                                        .description(1L),
                                fieldWithPath("userCoffeeStats[].nickname").type(JsonFieldType.STRING)
                                        .description("아스피"),
                                fieldWithPath("userCoffeeStats[].coffeeCount").type(JsonFieldType.NUMBER)
                                        .description("3")
                        )
                ));
    }

    @DisplayName("모임의 커피스택을 비운다.")
    @Test
    void useCoffeeStack() throws Exception {
        // given
        final Long meetingId = 1L;
        final Long userId = 1L;
        final UserAttendanceRequest request = new UserAttendanceRequest(true);
        validateToken("1");
        performPut("/meetings/" + meetingId + "/users/" + userId, request);

        // when
        final ResultActions resultActions = performPost("/meetings/" + meetingId + "/coffees/use");

        // then
        resultActions.andExpect(status().isNoContent())
                .andDo(document("meeting/use-coffee"));
    }

    @DisplayName("출석부를 조회한다.")
    @Test
    void showAttendances() throws Exception {
        // given
        final Long meetingId = 1L;
        final Long userId = 1L;
        validateToken(String.valueOf(userId));
        final AttendancesResponse attendancesResponse = new AttendancesResponse(
                List.of(
                        new AttendanceResponse(1L, "썬", "none"),
                        new AttendanceResponse(3L, "필즈", "none"),
                        new AttendanceResponse(5L, "포키", "present")
                )
        );
        given(attendanceService.findTodayAttendancesByMeeting(any(Long.class)))
                .willReturn(attendancesResponse);
        // when
        final ResultActions resultActions = performGet("/meetings/" + meetingId + "/attendances/today");

        // then
        resultActions.andExpect(status().isOk())
                .andDo(document("attendance/show",
                        responseFields(
                                fieldWithPath("users[].id").type(JsonFieldType.NUMBER)
                                        .description(1L),
                                fieldWithPath("users[].nickname").type(JsonFieldType.STRING)
                                        .description("썬"),
                                fieldWithPath("users[].attendanceStatus").type(JsonFieldType.STRING)
                                        .description("none")
                        )));
    }

    @DisplayName("오늘의 이벤트가 존재하지 않을 때 출석부를 조회할 경우 예외가 발생한다.")
    @Test
    void showAttendances_throwsException_ifEventNotExists() throws Exception {
        // given
        final Long meetingId = 1L;
        final Long userId = 1L;
        validateToken(String.valueOf(userId));

        given(attendanceService.findTodayAttendancesByMeeting(any(Long.class)))
                .willThrow(new ClientRuntimeException("오늘의 일정이 존재하지 않아 출석부를 조회할 수 없습니다.", HttpStatus.BAD_REQUEST));

        // when
        final ResultActions resultActions = performGet("/meetings/" + meetingId + "/attendances/today");

        // then
        resultActions.andExpect(status().isBadRequest())
                .andDo(document("attendance/show-event-not-exists",
                        responseFields(
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("오늘의 일정이 존재하지 않아 출석부를 조회할 수 없습니다.")
                        )));
    }
}
