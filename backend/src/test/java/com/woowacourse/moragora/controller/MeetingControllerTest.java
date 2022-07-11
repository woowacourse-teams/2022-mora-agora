package com.woowacourse.moragora.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowacourse.moragora.dto.MeetingRequest;
import com.woowacourse.moragora.dto.MeetingResponse;
import com.woowacourse.moragora.dto.UserResponse;
import com.woowacourse.moragora.exception.meeting.IllegalStartEndDateException;
import com.woowacourse.moragora.service.MeetingService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {MeetingController.class})
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeetingService meetingService;

    @DisplayName("미팅 방을 생성한다.")
    @Test
    void add() throws Exception {
        // given
        final MeetingRequest meetingRequest = new MeetingRequest(
                "모임1",
                LocalDate.of(2022, 7, 10),
                LocalDate.of(2022, 8, 10),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );

        // when
        given(meetingService.save(any(MeetingRequest.class)))
                .willReturn(1L);

        // then
        mockMvc.perform(post("/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", equalTo("/meetings/" + 1)));
    }

    @DisplayName("미팅 방을 생성 시 시작 날짜보다 종료 날짜가 이른 경우 예외가 발생한다.")
    @Test
    void add_throwsException_ifStartDateIsLaterThanEndDate() throws Exception {
        // given
        final MeetingRequest meetingRequest = new MeetingRequest(
                "모임1",
                LocalDate.of(2022, 7, 10),
                LocalDate.of(2022, 6, 10),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );

        // when
        given(meetingService.save(any(MeetingRequest.class)))
                .willThrow(new IllegalStartEndDateException());

        // then
        mockMvc.perform(post("/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message")
                        .value("시작 날짜보다 종료 날짜가 이를 수 없습니다."));
    }

    @DisplayName("미팅 방 이름의 길이가 50자를 초과할 경우 예외가 발생한다.")
    @ParameterizedTest
    @ValueSource(strings = {"012345678901234567890123456789012345678901234567891",
            "영일이삼사오육칠팔구영일이삼사오육칠팔구영일이삼사오육칠팔구영일이삼사오육칠팔구영일이삼사오육칠팔구영",
            "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghija"})
    void add_throwsException_ifMeetingNameTooLong(final String name) throws Exception {
        // given
        final MeetingRequest meetingRequest = new MeetingRequest(
                name,
                LocalDate.of(2022, 7, 10),
                LocalDate.of(2022, 8, 10),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );

        // when, then
        mockMvc.perform(post("/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message")
                        .value("모임 이름은 50자를 초과할 수 없습니다."));
    }

    @DisplayName("날짜 또는 시간의 입력 형식이 올바르지 않은 경우 예외가 발생한다.")
    @ParameterizedTest
    @CsvSource(value = {
            "2022-02,2022-02-05,12:00,23:00",
            "2022-02-02,2022-02,12:00,23:00",
            "2022-02-02,2022-02-05,12,23:00",
            "2022-02-02,2022-02-05,12:00,23",
    })
    void add_throwsException_ifInvalidDateTimeFormat(final String startDate,
                                                     final String endDate,
                                                     final String entranceTime,
                                                     final String leaveTime) throws Exception {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("name", "모임1");
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("entranceTime", entranceTime);
        params.put("leaveTime", leaveTime);

        // when, then
        mockMvc.perform(post("/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message")
                        .value("입력 형식이 올바르지 않습니다."));
    }

    // TODO userResponse 테스트 작성
    @DisplayName("단일 미팅 방을 조회한다.")
    @Test
    void findOne() throws Exception {
        // given
        final List<UserResponse> usersResponse = new ArrayList<>();
        usersResponse.add(new UserResponse(1L, "abc@naver.com", "foo", 5));
        usersResponse.add(new UserResponse(2L, "def@naver.com", "boo", 8));

        final MeetingResponse meetingResponse = new MeetingResponse(
                1L,
                "모임1",
                0,
                LocalDate.of(2022, 7, 10),
                LocalDate.of(2022, 8, 10),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                usersResponse
        );

        // when
        given(meetingService.findById(1L))
                .willReturn(meetingResponse);

        // then
        mockMvc.perform(get("/meetings/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.name", equalTo("모임1")))
                .andExpect(jsonPath("$.attendanceCount", equalTo(0)))
                .andExpect(jsonPath("$.startDate", equalTo("2022-07-10")))
                .andExpect(jsonPath("$.endDate", equalTo("2022-08-10")))
                .andExpect(jsonPath("$.entranceTime", equalTo("10:00:00")))
                .andExpect(jsonPath("$.leaveTime", equalTo("18:00:00")))
                .andExpect(jsonPath("$.leaveTime", equalTo("18:00:00")));
    }
}
