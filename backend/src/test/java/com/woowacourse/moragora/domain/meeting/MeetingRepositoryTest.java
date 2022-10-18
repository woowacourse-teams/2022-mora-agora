package com.woowacourse.moragora.domain.meeting;

import static com.woowacourse.moragora.support.fixture.MeetingFixtures.MORAGORA;
import static com.woowacourse.moragora.support.fixture.UserFixtures.KUN;
import static com.woowacourse.moragora.support.fixture.UserFixtures.SUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.woowacourse.moragora.domain.participant.Participant;
import com.woowacourse.moragora.domain.user.User;
import com.woowacourse.moragora.support.DataSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;


@Import(DataSupport.class)
@DataJpaTest(showSql = false)
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private DataSupport dataSupport;

    @DisplayName("미팅 방을 저장한다.")
    @Test
    void save() {
        // given
        final Meeting meeting = MORAGORA.create();

        // when
        final Meeting savedMeeting = meetingRepository.save(meeting);

        // then
        assertThat(savedMeeting.getId()).isNotNull();
    }

    @DisplayName("미팅 정보를 조회한다.")
    @Test
    void findById() {
        // given, when
        final Meeting meeting = meetingRepository.save(MORAGORA.create());
        final Meeting foundMeeting = meetingRepository.findById(meeting.getId())
                .get();

        // then
        assertThat(foundMeeting).isNotNull();
    }

    @DisplayName("미팅 정보와 모든 참가자 정보를 조회한다.")
    @Test
    void findMeetingAndParticipantsById() {
        // given
        final User user1 = dataSupport.saveUser(SUN.create());
        final User user2 = dataSupport.saveUser(KUN.create());
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());
        final Participant participant1 = dataSupport.saveParticipant(user1, meeting);
        final Participant participant2 = dataSupport.saveParticipant(user2, meeting);
        participant1.mapMeeting(meeting);
        participant2.mapMeeting(meeting);

        // when
        final Meeting foundMeeting = meetingRepository.findMeetingAndParticipantsById(meeting.getId()).get();

        // then
        assertAll(
                () -> assertThat(foundMeeting).isEqualTo(meeting),
                () -> assertThat(meeting.getParticipants()).containsAll(List.of(participant1, participant2))
        );
    }
}
