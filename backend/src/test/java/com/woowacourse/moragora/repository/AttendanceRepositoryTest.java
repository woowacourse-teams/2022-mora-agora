package com.woowacourse.moragora.repository;

import static com.woowacourse.moragora.support.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.UserFixtures.AZPI;
import static com.woowacourse.moragora.support.UserFixtures.KUN;
import static org.assertj.core.api.Assertions.assertThat;

import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Event;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.support.DataSupport;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import(DataSupport.class)
@DataJpaTest
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private DataSupport dataSupport;

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("미팅 참가자의 해당 날짜 출석정보를 조회한다.")
    @Test
    void findByParticipantIdAndEventId() {
        // given
        final Meeting meeting = MORAGORA.create();
        final Participant participant = dataSupport.saveParticipant(KUN.create(), meeting, false);
        final Event event1 = EVENT1.create(meeting);
        final Event savedEvent = eventRepository.save(event1);

        attendanceRepository.save(new Attendance(Status.TARDY, false, participant, savedEvent));

        final Optional<Event> event = eventRepository.findByMeetingIdAndDate(meeting.getId(), savedEvent.getDate());

        // when
        final Optional<Attendance> attendance = attendanceRepository
                .findByParticipantIdAndEventId(participant.getId(), event.get().getId());

        // then
        assertThat(attendance.isPresent()).isTrue();
    }

    @DisplayName("미팅 참가자들의 출석정보 목록을 조회한다.")
    @Test
    void findByParticipantIdIn() {
        // given
        final User user1 = KUN.create();
        final User user2 = AZPI.create();

        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());

        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);

        final Event event = EVENT1.create(meeting);
        final Event savedEvent = eventRepository.save(event);

        attendanceRepository.save(new Attendance(Status.TARDY, true, participant1, savedEvent));
        attendanceRepository.save(new Attendance(Status.TARDY, true, participant2, savedEvent));

        final List<Participant> participants = List.of(participant1, participant2);

        final List<Long> participantIds = participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());

        // when
        final List<Attendance> attendances = attendanceRepository.findByParticipantIdIn(participantIds);

        // then
        assertThat(attendances).hasSize(2);
    }

    @DisplayName("미팅 참가자들의 해당 날짜 출석정보 목록을 조회한다.")
    @Test
    void findByParticipantIdInAndEventId() {
        // given
        final User user1 = KUN.create();
        final User user2 = AZPI.create();

        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());

        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);

        final Event event = EVENT1.create(meeting);
        final Event savedEvent = eventRepository.save(event);

        final Attendance attendance1 = dataSupport.saveAttendance(participant1, savedEvent, Status.TARDY);

        final List<Participant> participants = List.of(participant1, participant2);

        final List<Long> participantIds = participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());

        // when
        final List<Attendance> attendances =
                attendanceRepository.findByParticipantIdInAndEventId(participantIds, attendance1.getEvent().getId());

        // then
        assertThat(attendances).hasSize(1);
    }

    @DisplayName("특정 참가자의 출석 데이터를 삭제한다.")
    @Test
    void deleteByParticipantId() {
        // given
        final User user = KUN.create();
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final Participant participant = dataSupport.saveParticipant(user, meeting);
        final Event event = eventRepository.save(EVENT1.create(meeting));
        dataSupport.saveAttendance(participant, event, Status.TARDY);

        // when
        attendanceRepository.deleteByParticipantId(participant.getId());
        final List<Attendance> result = attendanceRepository.findByParticipantIdIn(List.of(participant.getId()));

        // then
        assertThat(result).hasSize(0);
    }

}
