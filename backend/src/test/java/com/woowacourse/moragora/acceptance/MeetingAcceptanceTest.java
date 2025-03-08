package com.woowacourse.moragora.acceptance;

import static com.woowacourse.moragora.support.fixture.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.fixture.EventFixtures.EVENT_WITHOUT_DATE;
import static com.woowacourse.moragora.support.fixture.MeetingFixtures.F12;
import static com.woowacourse.moragora.support.fixture.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.fixture.UserFixtures.KUN;
import static com.woowacourse.moragora.support.fixture.UserFixtures.MASTER;
import static com.woowacourse.moragora.support.fixture.UserFixtures.PHILLZ;
import static com.woowacourse.moragora.support.fixture.UserFixtures.createUsers;
import static com.woowacourse.moragora.support.fixture.UserFixtures.getEmailsIncludingMaster;
import static com.woowacourse.moragora.support.fixture.UserFixtures.getNicknamesIncludingMaster;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.woowacourse.moragora.domain.event.Event;
import com.woowacourse.moragora.domain.meeting.Meeting;
import com.woowacourse.moragora.domain.user.User;
import com.woowacourse.moragora.dto.request.meeting.BeaconRequest;
import com.woowacourse.moragora.dto.request.meeting.BeaconsRequest;
import com.woowacourse.moragora.dto.request.meeting.MasterRequest;
import com.woowacourse.moragora.dto.request.meeting.MeetingRequest;
import com.woowacourse.moragora.dto.request.meeting.MeetingUpdateRequest;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("모임 관련 기능")
class MeetingAcceptanceTest extends AcceptanceTest {

    @DisplayName("사용자가 모임을 등록하고 상태코드 200 OK 를 반환받는다.")
    @Test
    void add() {
        // given
        final List<Long> userIds = saveUsers(createUsers());

        final Meeting meeting = MORAGORA.create();
        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(userIds)
                .build();

        // when
        final ValidatableResponse response = post("/meetings", meetingRequest, signUpAndGetToken(MASTER.create()));

        // then
        response.statusCode(HttpStatus.CREATED.value())
                .header("Location", notNullValue());
    }

    @DisplayName("사용자가 특정 모임을 조회하면 해당 모임 상세 정보와 상태코드 200을 반환한다.")
    @Test
    void findOne() {
        // given
        final User loginUser = MASTER.create();
        final Long loginId = signUp(loginUser);
        final String token = login(loginUser);

        final List<User> users = createUsers();
        final List<Long> userIds = saveUsers(users);

        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, userIds, meeting);

        userIds.add(loginId);

        final List<Integer> ids = userIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

        given(serverTimeManager.getDate())
                .willReturn(LocalDate.of(2022, 8, 1));

        // when
        final ValidatableResponse response = get("/meetings/" + meetingId, token);

        // then
        response.statusCode(HttpStatus.OK.value())
                .body("id", equalTo(meetingId))
                .body("name", equalTo(meeting.getName()))
                .body("attendedEventCount", equalTo(0))
                .body("isLoginUserMaster", equalTo(true))
                .body("isCoffeeTime", equalTo(false))
                .body("users.id", equalTo(ids))
                .body("users.nickname", equalTo(getNicknamesIncludingMaster()))
                .body("users.email", equalTo(getEmailsIncludingMaster()));
    }

    @DisplayName("사용자가 자신이 속한 모든 모임을 조회하면 모임 정보와 상태코드 200을 반환한다.")
    @Test
    void findMy() {
        // given
        final String token = signUpAndGetToken(MASTER.create());
        final Meeting meeting1 = MORAGORA.create();
        final Meeting meeting2 = F12.create();

        final User user = KUN.create();
        final List<Long> ids = saveUsers(List.of(user));

        final int meetingId1 = saveMeeting(token, ids, meeting1);
        final int meetingId2 = saveMeeting(token, ids, meeting2);

        final LocalDate today = LocalDate.now();
        final LocalTime time = LocalTime.of(10, 0);
        final LocalDateTime now = LocalDateTime.of(today, time.plusMinutes(1));

        final Event event = EVENT_WITHOUT_DATE.createEventOnDateAndTime(meeting1, today, time);

        given(serverTimeManager.getDate())
                .willReturn(event.getDate());
        given(serverTimeManager.getDateAndTime()).willReturn(now);
        given(serverTimeManager.isAttendanceOpen(time))
                .willReturn(true);
        given(serverTimeManager.calculateOpenTime(event.getStartTime()))
                .willReturn(LocalTime.of(9, 30));
        given(serverTimeManager.calculateAttendanceCloseTime(event.getStartTime()))
                .willReturn(LocalTime.of(10, 5));

        saveEvents(token, List.of(event), (long) meetingId1);

        // when
        attendanceScheduler.updateToTardyAtAttendanceClosingTime();
        final ValidatableResponse response = get("/meetings/me", token);

        // then
        response.statusCode(HttpStatus.OK.value())
                .body("meetings.id", containsInAnyOrder(meetingId1, meetingId2))
                .body("meetings.name", containsInAnyOrder(meeting1.getName(), meeting2.getName()))
                .body("meetings.tardyCount", containsInAnyOrder(1, 0))
                .body("meetings.isLoginUserMaster", containsInAnyOrder(true, true))
                .body("meetings.isCoffeeTime", containsInAnyOrder(true, false))
                .body("meetings.isActive", containsInAnyOrder(true, false))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.id", equalTo(1))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.attendanceOpenTime", equalTo("09:30"))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.attendanceClosedTime", equalTo("10:05"))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.meetingStartTime", equalTo("10:00"))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.meetingEndTime", equalTo("23:59"))
                .body("meetings.find{it.id == " + meetingId1 + "}.upcomingEvent.date",
                        equalTo(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .body("meetings.find{it.id == " + meetingId2 + "}.upcomingEvent", equalTo(null));
    }

    @DisplayName("마스터가 다른 참가자에게 모임 권한 넘기기를 요청하면 상태코드 204를 반환한다.")
    @Test
    void passMaster() {
        // given
        final String token = signUpAndGetToken(MASTER.create());
        final User user = KUN.create();
        final Long id = signUp(user);

        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, List.of(id), meeting);

        final MasterRequest masterRequest = new MasterRequest(id);

        // when
        final ValidatableResponse response = put("/meetings/" + meetingId + "/master", masterRequest, token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("마스터가 미팅 이름을 수정하면 상태코드 204를 반환한다.")
    @Test
    void changeName() {
        // given
        final User master = MASTER.create();
        final String token = signUpAndGetToken(master);

        final List<User> users = createUsers();
        final List<Long> userIds = saveUsers(users);
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, userIds, meeting);

        final MeetingUpdateRequest meetingUpdateRequest = new MeetingUpdateRequest("체크메이트");

        // when
        final ValidatableResponse response = put("meetings/" + meetingId, meetingUpdateRequest, token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("로그인한 유저가 자신이 속한 미팅에 대해 나가기를 요청하면 상태코드 204를 반환한다.")
    @Test
    void deleteMeFrom() {
        // given
        final User user = KUN.create();
        final Long id = signUp(user);
        final String token = login(user);

        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(signUpAndGetToken(MASTER.create()), List.of(id), meeting);

        // when
        final ValidatableResponse response = delete("/meetings/" + meetingId + "/me", token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("마스터인 사용자가 미팅 삭제를 하려고 하면 모임이 삭제가 완료되고 상태코드 204를 반환받는다.")
    @Test
    void delete() {
        // given
        final User user = MASTER.create();
        final String token = signUpAndGetToken(user);
        final List<Long> userIds = saveUsers(createUsers());
        final int meetingId = saveMeeting(token, userIds, MORAGORA.create());
        saveBeacons(token, (long) meetingId);

        // when
        final ValidatableResponse response = delete("/meetings/" + meetingId, token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("현재 미팅이 체크인 활성화 상태인지 확인하고 상태코드 200을 반환한다.")
    @Test
    void isActive() {
        // given
        given(serverTimeManager.getDate())
                .willReturn(LocalDate.of(2022, 8, 1));
        given(serverTimeManager.isAttendanceOpen(any()))
                .willReturn(true);

        final User master = MASTER.create();
        final String masterToken = signUpAndGetToken(master);
        final List<Long> userIds = saveUsers(createUsers());

        final Meeting meeting = MORAGORA.create();

        final int meetingId = saveMeeting(masterToken, userIds, meeting);
        final Event event = EVENT1.create(meeting);
        saveEvents(masterToken, List.of(event), (long) meetingId);

        // when
        final ValidatableResponse response = get("/meetings/" + meetingId + "/active", masterToken);

        // then
        response.statusCode(HttpStatus.OK.value());
    }

    @DisplayName("위치 기반 미팅에서 비콘을 저장하고 응답으로 204를 반환한다.")
    @Test
    void addBeacons() {
        // given
        final User master = MASTER.create();
        final String masterToken = signUpAndGetToken(master);
        final User user = PHILLZ.create();
        final Long userId = signUp(user);
        final int meetingId = saveMeeting(masterToken, List.of(userId), MORAGORA.create());

        final BeaconRequest beaconRequest = new BeaconRequest("서울시 송파구", 37.33, 126.58, 50);
        final BeaconsRequest beaconsRequest = new BeaconsRequest(List.of(beaconRequest));

        // when
        final String uri = String.format("/meetings/%d/beacons", meetingId);
        final ValidatableResponse response = post(uri, beaconsRequest, masterToken);

        // then
        response.statusCode(HttpStatus.CREATED.value())
                .header("Location", notNullValue());
    }
}
