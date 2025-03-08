package com.woowacourse.moragora.acceptance;

import static com.woowacourse.moragora.support.fixture.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.fixture.EventFixtures.EVENT_WITHOUT_DATE;
import static com.woowacourse.moragora.support.fixture.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.fixture.UserFixtures.FORKY;
import static com.woowacourse.moragora.support.fixture.UserFixtures.KUN;
import static com.woowacourse.moragora.support.fixture.UserFixtures.MASTER;
import static com.woowacourse.moragora.support.fixture.UserFixtures.NO_MASTER;
import static com.woowacourse.moragora.support.fixture.UserFixtures.PHILLZ;
import static com.woowacourse.moragora.support.fixture.UserFixtures.SUN;
import static com.woowacourse.moragora.support.fixture.UserFixtures.createUsers;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.woowacourse.moragora.domain.event.Event;
import com.woowacourse.moragora.domain.meeting.Meeting;
import com.woowacourse.moragora.domain.user.User;
import com.woowacourse.moragora.dto.request.meeting.BeaconRequest;
import com.woowacourse.moragora.dto.request.meeting.BeaconsRequest;
import com.woowacourse.moragora.dto.request.meeting.GeolocationAttendanceRequest;
import com.woowacourse.moragora.dto.request.user.UserAttendanceRequest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class AttendanceAcceptanceTest extends AcceptanceTest {

    @DisplayName("오늘의 출석부를 요청하면 날짜에 해당하는 출석부와 상태코드 200을 반환한다.")
    @Test
    void showAttendance() {
        // given
        final User user1 = SUN.create();
        final User user2 = KUN.create();
        final User user3 = FORKY.create();
        final List<Long> userIds = saveUsers(List.of(user2, user3));
        final String token = signUpAndGetToken(user1);
        final Meeting meeting = MORAGORA.create();
        final Long meetingId = (long) saveMeeting(token, userIds, meeting);
        final LocalTime startTime = LocalTime.now().minusHours(1);
        final Event event = Event.builder()
                .date(LocalDate.now())
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .meeting(meeting)
                .build();

        given(serverTimeManager.isAfter(any(LocalTime.class)))
                .willReturn(false);
        given(serverTimeManager.getDate())
                .willReturn(LocalDate.now());
        given(serverTimeManager.calculateAttendanceCloseTime(any(LocalTime.class)))
                .willReturn(startTime.plusHours(2));
        saveEvents(token, List.of(event), meetingId);

        // when
        final ValidatableResponse response = get("/meetings/" + meetingId + "/attendances/today", token);

        // then
        response.statusCode(HttpStatus.OK.value())
                .body("users.nickname", containsInAnyOrder(SUN.getNickname(), KUN.getNickname(), FORKY.getNickname()))
                .body("users.attendanceStatus", containsInAnyOrder("none", "none", "none"));
    }

    @DisplayName("마감된 오늘의 출석부를 요청하면 NONE이었던 미팅 멤버들이 TARDY로 변경되고 상태코드 200을 반환한다.")
    @Test
    void showAttendanceAfterClosedTime() {
        // given
        final User user1 = SUN.create();
        final User user2 = KUN.create();
        final User user3 = FORKY.create();
        final List<Long> userIds = saveUsers(List.of(user2, user3));
        final String token = signUpAndGetToken(user1);
        final Meeting meeting = MORAGORA.create();
        final Long meetingId = (long) saveMeeting(token, userIds, meeting);
        final LocalDateTime dateTime = LocalDate.now().atTime(10, 0);
        final Event event = Event.builder()
                .date(dateTime.toLocalDate())
                .startTime(dateTime.toLocalTime())
                .endTime(dateTime.toLocalTime().plusHours(8))
                .meeting(meeting)
                .build();
        given(serverTimeManager.getDate())
                .willReturn(dateTime.toLocalDate());
        given(serverTimeManager.calculateAttendanceCloseTime(any(LocalTime.class)))
                .willReturn(dateTime.toLocalTime().plusMinutes(5));
        saveEvents(token, List.of(event), meetingId);

        // when
        attendanceScheduler.updateToTardyAtAttendanceClosingTime();
        final ValidatableResponse response = get("/meetings/" + meetingId + "/attendances/today", token);

        // then
        await().untilAsserted(() ->
                response.statusCode(HttpStatus.OK.value())
                        .body("users.nickname",
                                containsInAnyOrder(SUN.getNickname(), KUN.getNickname(), FORKY.getNickname()))
                        .body("users.attendanceStatus", containsInAnyOrder("tardy", "tardy", "tardy")));
    }

    @DisplayName("오늘의 출석부를 요청할 때 오늘의 이벤트가 없다면 상태코드 404를 반환한다.")
    @Test
    void showAttendance_throwsException_ifEventNotExists() {
        // given
        final User user1 = SUN.create();
        final User user2 = KUN.create();
        final User user3 = FORKY.create();
        final List<Long> userIds = saveUsers(List.of(user2, user3));
        final String token = signUpAndGetToken(user1);
        final Meeting meeting = MORAGORA.create();
        final Long meetingId = (long) saveMeeting(token, userIds, meeting);

        given(serverTimeManager.getDate())
                .willReturn(EVENT1.getDate());
        given(serverTimeManager.calculateAttendanceCloseTime(any(LocalTime.class)))
                .willReturn(LocalTime.of(10, 30));
        saveEvents(token, List.of(EVENT1.create(meeting)), meetingId);

        given(serverTimeManager.getDate())
                .willReturn(EVENT1.getDate().plusDays(1));

        // when
        final ValidatableResponse response = get("/meetings/" + meetingId + "/attendances/today", token);

        // then
        response.statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("오늘의 일정이 존재하지 않아 출석부를 조회할 수 없습니다."));
    }

    @DisplayName("미팅 참가자의 출석을 업데이트하면 상태코드 204을 반환한다.")
    @Test
    void updateAttendance() {
        // given
        final UserAttendanceRequest userAttendanceRequest = new UserAttendanceRequest(true);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 1);

        given(serverTimeManager.isAttendanceOpen(any(LocalTime.class)))
                .willReturn(true);
        given(serverTimeManager.getDate())
                .willReturn(dateTime.toLocalDate());
        given(serverTimeManager.getDateAndTime())
                .willReturn(dateTime);
        given(serverTimeManager.calculateAttendanceCloseTime(any(LocalTime.class)))
                .willReturn(dateTime.toLocalTime().plusMinutes(30));

        final String token = signUpAndGetToken(MASTER.create());

        final List<Long> userIds = saveUsers(createUsers());
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, userIds, meeting);

        saveEvents(token, List.of(EVENT1.create(meeting)), (long) meetingId);

        // when
        final ValidatableResponse response = post(
                "/meetings/" + meetingId + "/users/" + userIds.get(0) + "/attendances/today",
                userAttendanceRequest, token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("마스터가 아닌 참가자가 미팅 참가자의 출석을 업데이트하면 상태코드 403을 반환한다.")
    @Test
    void updateAttendance_NotMaster() {
        // given
        final UserAttendanceRequest userAttendanceRequest = new UserAttendanceRequest(true);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 1);

        given(serverTimeManager.getDate())
                .willReturn(dateTime.toLocalDate());
        given(serverTimeManager.getDateAndTime())
                .willReturn(dateTime);

        final String masterToken = signUpAndGetToken(MASTER.create());
        final User noMaster = NO_MASTER.create();
        final Long noMasterId = signUp(noMaster);
        final String noMasterToken = login(noMaster);

        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(masterToken, List.of(noMasterId), meeting);

        saveEvents(masterToken, List.of(EVENT1.create(meeting)), (long) meetingId);

        // when
        final ValidatableResponse response = post(
                "/meetings/" + meetingId + "/users/" + 1 + "/attendances/today",
                userAttendanceRequest, noMasterToken);

        // then
        response.statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", equalTo("마스터 권한이 없습니다."));
    }

    @DisplayName("모임에 쌓인 커피스택을 사용하면 참가자들의 커피 스택을 차감하고 상태코드 204을 반환한다.")
    @Test
    void useCoffeeStack() {
        // given
        final String token = signUpAndGetToken(MASTER.create());
        final List<Long> userIds = saveUsers(createUsers());
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, userIds, meeting);

        final LocalDate date = LocalDate.now();
        final LocalTime time = LocalTime.of(10, 0);
        final LocalDateTime now = LocalDateTime.of(date, time.plusMinutes(1));
        final Event event = EVENT_WITHOUT_DATE.createEventOnDateAndTime(meeting, date, time);

        given(serverTimeManager.getDate()).willReturn(date);
        given(serverTimeManager.getDateAndTime()).willReturn(now);

        saveEvents(token, List.of(event), (long) meetingId);

        // when
        attendanceScheduler.updateToTardyAtAttendanceClosingTime();
        final ValidatableResponse response = RestAssured.given().log().all()
                .auth().oauth2(token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/meetings/" + meetingId + "/coffees/use")
                .then().log().all();

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("마스터가 아닌 유저가 커피스택 비우기를 요청시 상태코드 403울 반환한다.")
    @Test
    void useCoffeeStack_NotMaster() {
        // given
        final String masterToken = signUpAndGetToken(MASTER.create());
        final User noMaster = NO_MASTER.create();
        final Long noMasterId = signUp(noMaster);
        final String noMasterToken = login(noMaster);

        final List<User> users = createUsers();
        final List<Long> userIds = saveUsers(users);
        userIds.add(noMasterId);
        final int meetingId = saveMeeting(masterToken, userIds, MORAGORA.create());

        // when
        final ValidatableResponse response = RestAssured.given().log().all()
                .auth().oauth2(noMasterToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/meetings/" + meetingId + "/coffees/use")
                .then().log().all();

        // then
        response.statusCode(HttpStatus.FORBIDDEN.value());
    }

    @DisplayName("모임에 쌓인 커피스택을 조회하면 참가자별 다음에 비워질 커피스택 정보와 상태코드 200을 반환한다.")
    @Test
    void showUserCoffeeStats() {
        // given
        final User master = MASTER.create();
        final Long masterId = signUp(master);
        final String token = login(master);
        final LocalDate date = LocalDate.now();
        final LocalTime time = LocalTime.of(10, 0);
        final LocalDateTime now = LocalDateTime.of(date, time.plusMinutes(1));

        final List<Long> userIds = saveUsers(createUsers());
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(token, userIds, meeting);
        final Event event = EVENT_WITHOUT_DATE.createEventOnDateAndTime(meeting, date, time);

        given(serverTimeManager.getDate()).willReturn(date);
        given(serverTimeManager.getDateAndTime()).willReturn(now);
        saveEvents(token, List.of(event), (long) meetingId);

        // when
        attendanceScheduler.updateToTardyAtAttendanceClosingTime();
        final ValidatableResponse response = get("/meetings/" + meetingId + "/coffees/use", token);

        // then
        response.statusCode(HttpStatus.OK.value())
                .body("userCoffeeStats.find{it.id == " + masterId + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(0) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(1) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(2) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(3) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(4) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(5) + "}.coffeeCount", equalTo(1))
                .body("userCoffeeStats.find{it.id == " + userIds.get(6) + "}.coffeeCount", equalTo(1));
    }

    @DisplayName("위치 기반으로 출석을 해서 성공할 경우 출석 상태 PRESENT로 변경한다")
    @Test
    void attendWithBeaconBase_ifAttendSuccess() {
        // given
        given(serverTimeManager.getDate())
                .willReturn(LocalDate.of(2022, 8, 1));
        given(serverTimeManager.isAttendanceOpen(any(LocalTime.class)))
                .willReturn(true);

        final User master = MASTER.create();
        final String masterToken = signUpAndGetToken(master);
        final User user = PHILLZ.create();
        final Long userId = signUp(user);
        final String token = login(user);
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(masterToken, List.of(userId), meeting);
        final Event event = EVENT1.create(meeting);
        saveEvents(masterToken, List.of(event), (long) meetingId);

        // create beacon
        final BeaconRequest beaconRequest = new BeaconRequest("루터회관", 37.5153, 127.103, 50);
        final BeaconsRequest beaconsRequest = new BeaconsRequest(List.of(beaconRequest));
        final String beaconUri = String.format("/meetings/%d/beacons", meetingId);
        post(beaconUri, beaconsRequest, masterToken);

        // when
        final GeolocationAttendanceRequest geoLocationAttendanceRequest =
                new GeolocationAttendanceRequest(37.5154, 127.1031);
        final String attendanceUri =
                String.format("/meetings/%d/users/%d/attendances/today/geolocation", meetingId, userId);
        final ValidatableResponse response = post(attendanceUri, geoLocationAttendanceRequest, token);

        // then
        response.statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("위치 기반으로 출석을 해서 실패할 경우 400을 반환한다.")
    @Test
    void attendWithBeaconBase_throwsException_ifAttendFail() {
        // given
        given(serverTimeManager.getDate())
                .willReturn(LocalDate.of(2022, 8, 1));
        given(serverTimeManager.isAttendanceOpen(any(LocalTime.class)))
                .willReturn(true);

        final User master = MASTER.create();
        final String masterToken = signUpAndGetToken(master);
        final User user = PHILLZ.create();
        final Long userId = signUp(user);
        final String token = login(user);
        final Meeting meeting = MORAGORA.create();
        final int meetingId = saveMeeting(masterToken, List.of(userId), meeting);
        final Event event = EVENT1.create(meeting);
        saveEvents(masterToken, List.of(event), (long) meetingId);

        final BeaconRequest beaconRequest1 = new BeaconRequest("서울역", 37.54788, 126.99712, 50);
        final BeaconRequest beaconRequest2 = new BeaconRequest("루터회관", 37.5153, 127.103, 50);
        final BeaconRequest beaconRequest3 = new BeaconRequest("선릉역", 37.50450, 127.048982, 50);
        final List<BeaconRequest> requestBody = List.of(beaconRequest1, beaconRequest2, beaconRequest3);
        final String beaconUri = String.format("/meetings/%d/beacons", meetingId);
        post(beaconUri, requestBody, masterToken);

        // when
        final var attendanceRequest = new GeolocationAttendanceRequest(37.5138, 127.1012); // 잠실역 8번
        final String attendanceUri = String.format("/meetings/%d/users/%d/attendances/today/geolocation", meetingId,
                userId);
        final ValidatableResponse response = post(attendanceUri, attendanceRequest, token);

        // then
        response.statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("비콘의 출석 반경 이내에 있지 않습니다."));
    }
}
