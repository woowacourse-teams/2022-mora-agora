package com.woowacourse.moragora.repository;

import static com.woowacourse.moragora.support.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.EventFixtures.EVENT2;
import static com.woowacourse.moragora.support.EventFixtures.EVENT3;
import static com.woowacourse.moragora.support.MeetingFixtures.F12;
import static com.woowacourse.moragora.support.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.UserFixtures.AZPI;
import static com.woowacourse.moragora.support.UserFixtures.KUN;
import static com.woowacourse.moragora.support.UserFixtures.SUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Event;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.support.DataSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import(DataSupport.class)
@DataJpaTest(showSql = false)
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private DataSupport dataSupport;

    @DisplayName("미팅 참가자의 해당 날짜 출석정보를 조회한다.")
    @Test
    void findByParticipantIdAndEventId() {
        // given
        final Meeting meeting = MORAGORA.create();
        final Participant participant = dataSupport.saveParticipant(KUN.create(), meeting, false);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));
        dataSupport.saveAttendance(participant, event, Status.TARDY);

        // when
        final Optional<Attendance> attendance = attendanceRepository
                .findByParticipantIdAndEventId(participant.getId(), event.getId());

        // then
        assertThat(attendance.isPresent()).isTrue();
    }

    @DisplayName("미팅 참가자들의 특정 날짜 이전의 출석 기록을 조회한다.")
    @Test
    void findByParticipantIdInAndDateLessThanEqual() {
        // given
        final User user1 = KUN.create();
        final User user2 = AZPI.create();
        final Meeting meeting = MORAGORA.create();

        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Event event2 = dataSupport.saveEvent(EVENT2.create(meeting));
        final Event event3 = dataSupport.saveEvent(EVENT3.create(meeting));

        dataSupport.saveAttendance(participant1, event1, Status.TARDY);
        dataSupport.saveAttendance(participant2, event1, Status.TARDY);
        dataSupport.saveAttendance(participant1, event2, Status.TARDY);
        dataSupport.saveAttendance(participant2, event2, Status.TARDY);
        dataSupport.saveAttendance(participant1, event3, Status.TARDY);
        dataSupport.saveAttendance(participant2, event3, Status.TARDY);

        // when
        final List<Attendance> attendances = attendanceRepository.findByParticipantIdInAndDateLessThanEqual(
                List.of(participant1.getId(), participant2.getId()),
                event2.getDate()
        );

        // then
        assertThat(attendances).hasSize(4);
    }

    @DisplayName("미팅 참가자들의 해당 날짜 출석정보 목록을 조회한다.")
    @Test
    void findByParticipantIdInAndEventId() {
        // given
        final User user1 = KUN.create();
        final User user2 = AZPI.create();
        final Meeting meeting = MORAGORA.create();

        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Event event2 = dataSupport.saveEvent(EVENT1.create(meeting));

        dataSupport.saveAttendance(participant1, event1, Status.TARDY);
        dataSupport.saveAttendance(participant2, event1, Status.TARDY);
        dataSupport.saveAttendance(participant1, event2, Status.TARDY);
        dataSupport.saveAttendance(participant2, event2, Status.TARDY);

        // when
        final List<Attendance> attendances = attendanceRepository.findByParticipantIdInAndEventId(
                List.of(participant1.getId(), participant2.getId()),
                event1.getId()
        );

        // then
        assertThat(attendances).hasSize(2);
    }

    @DisplayName("출석부의 상태가 NONE인 경우, TARDY로 변경한다.")
    @Test
    void updateAttendanceToTardy() {
        // given
        final User user1 = SUN.create();
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final Participant participant = dataSupport.saveParticipant(user1, meeting);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));
        final Long attendanceId = dataSupport.saveAttendance(participant, event, Status.NONE).getId();

        // when
        attendanceRepository.updateAttendanceToTardy(attendanceId);
        final Optional<Attendance> expected = attendanceRepository
                .findByParticipantIdAndEventId(participant.getId(), event.getId());
        assert (expected.isPresent());

        // then
        assertThat(expected.get().getStatus()).isEqualTo(Status.TARDY);
    }


    @DisplayName("이벤트로 사용자 출석정보 목록을 조회한다.")
    @Test
    void findByEventIdIn() {
        // given
        final User user1 = KUN.create();
        final User user2 = AZPI.create();

        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));

        dataSupport.saveAttendance(participant1, event, Status.TARDY);
        dataSupport.saveAttendance(participant2, event, Status.TARDY);

        // when
        final List<Attendance> attendances = attendanceRepository.findByEventIdIn(List.of(event.getId()));

        // then
        assertThat(attendances).hasSize(2);
    }

    @DisplayName("이벤트에 속한 출석 데이터를 삭제한다.")
    @Test
    void deleteByEventIdIn() {
        // given
        final User user = KUN.create();
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting);

        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting));
        final Event event2 = dataSupport.saveEvent(EVENT2.create(meeting));

        dataSupport.saveAttendance(participant, event1, Status.TARDY);
        dataSupport.saveAttendance(participant, event2, Status.TARDY);

        // when
        attendanceRepository.deleteByEventIdIn(List.of(event1.getId()));
        final List<Attendance> result = attendanceRepository
                .findByParticipantIdInAndDateLessThanEqual(List.of(participant.getId()), event2.getDate());

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("참가자의 출석 데이터를 삭제한다.")
    @Test
    void deleteByParticipantIdIn() {
        // given
        final User user = KUN.create();
        final Meeting meeting1 = dataSupport.saveMeeting(MORAGORA.create());
        final Meeting meeting2 = dataSupport.saveMeeting(F12.create());

        final Participant participant1 = dataSupport.saveParticipant(user, meeting1);
        final Participant participant2 = dataSupport.saveParticipant(user, meeting2);

        final Event event1 = dataSupport.saveEvent(EVENT1.create(meeting1));
        final Event event2 = dataSupport.saveEvent(EVENT1.create(meeting2));

        dataSupport.saveAttendance(participant1, event1, Status.TARDY);
        dataSupport.saveAttendance(participant2, event2, Status.TARDY);

        // when
        attendanceRepository.deleteByParticipantIdIn(List.of(participant1.getId()));
        final List<Attendance> result = attendanceRepository
                .findByParticipantIdInAndDateLessThanEqual(List.of(participant1.getId(), participant2.getId()),
                        event2.getDate());

        // then
        assertThat(result).hasSize(1);
    }

    @DisplayName("특정 참가자의 출석 데이터를 삭제한다.")
    @Test
    void deleteByParticipantId() {
        // given
        final User user = KUN.create();
        final Meeting meeting = MORAGORA.create();
        final Participant participant = dataSupport.saveParticipant(user, meeting);
        final Event event = dataSupport.saveEvent(EVENT1.create(meeting));
        dataSupport.saveAttendance(participant, event, Status.TARDY);

        // when
        attendanceRepository.deleteByParticipantId(participant.getId());
        final Optional<Attendance> result = attendanceRepository
                .findByParticipantIdAndEventId(participant.getId(), event.getId());

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("해당 참가자의 출석 데이터가 존재하지 않을 경우에 삭제해도 예외가 발생하지 않는다.")
    @Test
    void deleteByParticipantId_ifNotExist() {
        // given
        final User user = KUN.create();
        final Meeting meeting = MORAGORA.create();
        final Participant participant = dataSupport.saveParticipant(user, meeting);

        // when, then
        assertThatCode(() -> attendanceRepository.deleteByParticipantId(participant.getId()))
                .doesNotThrowAnyException();
    }
}
