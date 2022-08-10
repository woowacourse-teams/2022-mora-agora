package com.woowacourse.moragora.service;

import static com.woowacourse.moragora.support.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.EventFixtures.EVENT2;
import static com.woowacourse.moragora.support.EventFixtures.EVENT3;
import static com.woowacourse.moragora.support.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.MeetingFixtures.TEATIME;
import static com.woowacourse.moragora.support.UserFixtures.KUN;
import static com.woowacourse.moragora.support.UserFixtures.MASTER;
import static com.woowacourse.moragora.support.UserFixtures.PHILLZ;
import static com.woowacourse.moragora.support.UserFixtures.WOODY;
import static com.woowacourse.moragora.support.UserFixtures.createUsers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.woowacourse.moragora.dto.MeetingRequest;
import com.woowacourse.moragora.dto.MeetingResponse;
import com.woowacourse.moragora.dto.MyMeetingResponse;
import com.woowacourse.moragora.dto.MyMeetingsResponse;
import com.woowacourse.moragora.dto.ParticipantResponse;
import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Event;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.exception.participant.InvalidParticipantException;
import com.woowacourse.moragora.exception.user.UserNotFoundException;
import com.woowacourse.moragora.support.DataSupport;
import com.woowacourse.moragora.support.DatabaseCleanUp;
import com.woowacourse.moragora.support.ServerTimeManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeetingServiceTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ServerTimeManager serverTimeManager;

    @Autowired
    private DataSupport dataSupport;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.afterPropertiesSet();
        databaseCleanUp.execute();
    }

    @DisplayName("미팅 방을 저장한다.")
    @Test
    void save() {
        // given
        final User master = dataSupport.saveUser(MASTER.create());
        final List<Long> userIds = dataSupport.saveUsers(createUsers());
        final Meeting meeting = MORAGORA.create();

        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(userIds)
                .build();

        // when
        final Long expected = meetingService.save(meetingRequest, master.getId());

        // then
        assertThat(expected).isNotNull();
    }

    @DisplayName("미팅이 생성될 때, 참가자 명단에 미팅 생성자가 있는 경우 예외를 반환한다.")
    @Test
    void save_throwException_ifUserIdsContainLoginId() {
        // given
        final User master = dataSupport.saveUser(MASTER.create());
        final List<Long> userIds = dataSupport.saveUsers(createUsers());
        userIds.add(master.getId());

        final Meeting meeting = MORAGORA.create();

        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(userIds)
                .build();

        // when, then
        assertThatThrownBy(() -> meetingService.save(meetingRequest, master.getId()))
                .isInstanceOf(InvalidParticipantException.class);
    }

    @DisplayName("미팅이 생성될 때, 참가자 명단에 중복이 있는 경우 예외를 반환한다.")
    @Test
    void save_throwException_ifUserIdsDuplicated() {
        // given
        final User master = dataSupport.saveUser(MASTER.create());
        final User user = dataSupport.saveUser(KUN.create());

        final Meeting meeting = MORAGORA.create();
        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(List.of(user.getId(), user.getId()))
                .build();

        // when, then
        assertThatThrownBy(() -> meetingService.save(meetingRequest, master.getId()))
                .isInstanceOf(InvalidParticipantException.class);
    }

    @DisplayName("미팅이 생성될 때, 참가자 명단이 비어있는 경우 예외를 반환한다.")
    @Test
    void save_throwException_ifUserIdsBlank() {
        // given
        final User user = dataSupport.saveUser(MASTER.create());

        final Meeting meeting = MORAGORA.create();
        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(List.of(user.getId()))
                .build();

        // when, then
        assertThatThrownBy(() -> meetingService.save(meetingRequest, user.getId()))
                .isInstanceOf(InvalidParticipantException.class);
    }

    @DisplayName("미팅이 생성될 때, 참가자 명단에 존재하지 않는 user가 들어가있는 경우 예외를 반환한다.")
    @Test
    void save_throwException_ifNotExistIdInUserIds() {
        // given
        final User user = dataSupport.saveUser(MASTER.create());

        final Meeting meeting = MORAGORA.create();

        final MeetingRequest meetingRequest = MeetingRequest.builder()
                .name(meeting.getName())
                .userIds(List.of(99L))
                .build();

        // when, then
        assertThatThrownBy(() -> meetingService.save(meetingRequest, user.getId()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다.")
    @Test
    void findById() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user = dataSupport.saveUser(KUN.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting, true);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Event event2 = dataSupport.saveEvent(EVENT2.create(meeting));
        final Event event3 = dataSupport.saveEvent(EVENT3.create(meeting));
        dataSupport.saveAttendance(participant, event1, Status.TARDY);
        dataSupport.saveAttendance(participant, event2, Status.TARDY);
        dataSupport.saveAttendance(participant, event3, Status.TARDY);

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .usersResponse(List.of(ParticipantResponse.of(user, Status.TARDY, 3)))
                .attendanceCount(3)
                .isCoffeeTime(true)
                .isActive(false)
                .isMaster(true)
                .hasUpcomingEvent(false)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 4, 10, 6);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .ignoringFields("users")
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다_당일 출석부가 없는 경우 추가 후 조회한다.")
    @Test
    void findById_putAttendanceIfAbsent() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user = dataSupport.saveUser(KUN.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting);

        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Event event2 = dataSupport.saveEvent(EVENT2.create(meeting));
        final Event event3 = dataSupport.saveEvent(EVENT3.create(meeting));
        dataSupport.saveAttendance(participant, event1, Status.TARDY);
        dataSupport.saveAttendance(participant, event2, Status.TARDY);
        dataSupport.saveAttendance(participant, event3, Status.TARDY);

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .usersResponse(List.of(ParticipantResponse.of(user, Status.TARDY, 3)))
                .attendanceCount(3)
                .isCoffeeTime(true)
                .isActive(false)
                .isMaster(false)
                .hasUpcomingEvent(false)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 4, 10, 0);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .ignoringFields("users")
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다(출석 마감 시간 전에는 당일 지각 스택은 반영되지 않는다.)")
    @Test
    void findById_ifNotOverClosingTime() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user1 = dataSupport.saveUser(KUN.create());
        final Participant participant1 = dataSupport.saveParticipant(user1, meeting, true);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Attendance attendance1 = dataSupport.saveAttendance(participant1, event1, Status.TARDY);

        final User user2 = dataSupport.saveUser(PHILLZ.create());
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        final Attendance attendance2 = dataSupport.saveAttendance(participant2, event1, Status.TARDY);

        final User user3 = dataSupport.saveUser(WOODY.create());
        final Participant participant3 = dataSupport.saveParticipant(user3, meeting);
        final Attendance attendance3 = dataSupport.saveAttendance(participant3, event1, Status.TARDY);

        final List<ParticipantResponse> usersResponse = List.of(
                ParticipantResponse.of(user1, attendance1.getStatus(), 0),
                ParticipantResponse.of(user2, attendance2.getStatus(), 0),
                ParticipantResponse.of(user3, attendance3.getStatus(), 0)
        );

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .usersResponse(usersResponse)
                .attendanceCount(1)
                .isCoffeeTime(false)
                .isActive(true)
                .isMaster(true)
                .hasUpcomingEvent(true)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 4);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user1.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다(출석 마감 시간이 지나면 당일 지각 스택도 반영된다.)")
    @Test
    void findById_ifOverClosingTime() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user1 = dataSupport.saveUser(KUN.create());
        final Participant participant1 = dataSupport.saveParticipant(user1, meeting, true);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Attendance attendance1 = dataSupport.saveAttendance(participant1, event1, Status.TARDY);

        final User user2 = dataSupport.saveUser(PHILLZ.create());
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        final Attendance attendance2 = dataSupport.saveAttendance(participant2, event1, Status.TARDY);

        final User user3 = dataSupport.saveUser(WOODY.create());
        final Participant participant3 = dataSupport.saveParticipant(user3, meeting);
        final Attendance attendance3 = dataSupport.saveAttendance(participant3, event1, Status.TARDY);

        final List<ParticipantResponse> usersResponse = List.of(
                ParticipantResponse.of(user1, attendance1.getStatus(), 1),
                ParticipantResponse.of(user2, attendance2.getStatus(), 1),
                ParticipantResponse.of(user3, attendance3.getStatus(), 1)
        );

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .usersResponse(usersResponse)
                .attendanceCount(1)
                .isCoffeeTime(true)
                .isActive(false)
                .isMaster(true)
                .hasUpcomingEvent(true)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 6);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user1.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다(당일 일정이 없으면 출석부를 초기화 하지 않고 기존 출석 데이터를 응답한다).")
    @Test
    void findById_if_hasNoEvent_and_hasUpcomingEvent() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user = dataSupport.saveUser(KUN.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting, true);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));
        dataSupport.saveEvent(EVENT2.create(meeting));
        dataSupport.saveAttendance(participant, event, Status.PRESENT);

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .attendanceCount(1)
                .isCoffeeTime(false)
                .isActive(true)
                .isMaster(true)
                .hasUpcomingEvent(true)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 9, 59);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .ignoringFields("users")
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("id로 모임 상세 정보를 조회한다(당일부터의 일정이 없을 경우 기존의 출석 데이터를 응답한다).")
    @Test
    void findById_if_hasNoEvent_and_hasNoUpcomingEvent() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final User user = dataSupport.saveUser(KUN.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting, true);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));
        dataSupport.saveAttendance(participant, event, Status.PRESENT);

        final MeetingResponse expectedMeetingResponse = MeetingResponse.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .attendanceCount(1)
                .isCoffeeTime(false)
                .isActive(false)
                .isMaster(true)
                .hasUpcomingEvent(false)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 2, 9, 59);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .ignoringFields("users")
                .isEqualTo(expectedMeetingResponse);
    }

    @DisplayName("Master인 id로 모임 상세 정보를 조회한다")
    @Test
    void findById_isMaster() {
        // given
        final Meeting meeting = MORAGORA.create();
        final User user = KUN.create();

        final Participant participant = dataSupport.saveParticipant(user, meeting, true);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 5);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response.getIsMaster()).isTrue();
    }

    @DisplayName("Master가 아닌 id로 모임 상세 정보를 조회한다")
    @Test
    void findById_NotMaster() {
        // given
        final Meeting meeting = MORAGORA.create();
        final User user = KUN.create();

        final Participant participant = dataSupport.saveParticipant(user, meeting, false);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 7, 14, 10, 5);
        serverTimeManager.refresh(dateTime);

        // when
        final MeetingResponse response = meetingService.findById(meeting.getId(), user.getId());

        // then
        assertThat(response.getIsMaster()).isFalse();
    }

    @DisplayName("유저 id로 유저가 속한 모든 모임을 조회한다.")
    @Test
    void findAllByUserId() {
        // given
        final Meeting meeting1 = MORAGORA.create();
        final Meeting meeting2 = TEATIME.create();
        final User user = KUN.create();
        dataSupport.saveParticipant(user, meeting1, true);
        dataSupport.saveParticipant(user, meeting2, true);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting1));
        final Event event2 = dataSupport.saveEvent(EVENT1.create(meeting2));

        final MyMeetingResponse response1 = MyMeetingResponse.builder()
                .id(meeting1.getId())
                .name(meeting1.getName())
                .isActive(false)
                .entranceTime(event1.getEntranceTime())
                .closingTime(event1.getEntranceTime().plusMinutes(5))
                .tardyCount(0)
                .isMaster(true)
                .isCoffeeTime(false)
                .hasUpcomingEvent(true)
                .build();

        final MyMeetingResponse response2 = MyMeetingResponse.builder()
                .id(meeting2.getId())
                .name(meeting2.getName())
                .isActive(false)
                .entranceTime(event2.getEntranceTime())
                .closingTime(event2.getEntranceTime().plusMinutes(5))
                .tardyCount(0)
                .isMaster(true)
                .isCoffeeTime(false)
                .hasUpcomingEvent(true)
                .build();

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 1, 10, 5);
        serverTimeManager.refresh(dateTime);

        // when
        final MyMeetingsResponse response = meetingService.findAllByUserId(user.getId());

        // then
        assertThat(response).usingRecursiveComparison()
                .isEqualTo(new MyMeetingsResponse(List.of(response1, response2)));
    }
}
