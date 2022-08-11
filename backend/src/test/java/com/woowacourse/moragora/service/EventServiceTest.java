package com.woowacourse.moragora.service;

import static com.woowacourse.moragora.support.EventFixtures.EVENT1;
import static com.woowacourse.moragora.support.EventFixtures.EVENT2;
import static com.woowacourse.moragora.support.MeetingFixtures.MORAGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.woowacourse.moragora.dto.EventRequest;
import com.woowacourse.moragora.dto.EventResponse;
import com.woowacourse.moragora.dto.EventsRequest;
import com.woowacourse.moragora.entity.Event;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.exception.event.EventNotFoundException;
import com.woowacourse.moragora.support.DataSupport;
import com.woowacourse.moragora.support.DatabaseCleanUp;
import com.woowacourse.moragora.support.ServerTimeManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private DataSupport dataSupport;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ServerTimeManager serverTimeManager;

    @BeforeEach
    void setUp() {
        databaseCleanUp.afterPropertiesSet();
        databaseCleanUp.execute();
    }

    @DisplayName("모임 일정들을 저장한다.")
    @Test
    void save() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());

        final Event event1 = EVENT1.create(meeting);
        final Event event2 = EVENT2.create(meeting);

        final EventsRequest eventsRequest = new EventsRequest(
                List.of(
                        EventRequest.builder()
                                .meetingStartTime(event1.getStartTime())
                                .meetingEndTime(event1.getEndTime())
                                .date(event1.getDate())
                                .build()
                        ,
                        EventRequest.builder()
                                .meetingStartTime(event2.getStartTime())
                                .meetingEndTime(event2.getEndTime())
                                .date(event2.getDate())
                                .build()
                ));

        // when, then
        assertThatCode(() -> eventService.save(eventsRequest, meeting.getId()))
                .doesNotThrowAnyException();
    }

    @DisplayName("모임의 가장 가까운 일정을 조회한다.")
    @Test
    void findUpcomingEvent() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());

        final Event event1 = EVENT1.create(meeting);
        final Event event2 = EVENT2.create(meeting);
        dataSupport.saveEvent(event1);
        dataSupport.saveEvent(event2);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 7, 30, 10, 6);
        serverTimeManager.refresh(dateTime);

        final EventResponse expected = EventResponse.of(
                event1, LocalTime.of(9, 30), LocalTime.of(10, 5));

        // when
        final EventResponse actual = eventService.findUpcomingEvent(meeting.getId());

        // then
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @DisplayName("모임의 가장 가까운 일정을 조회했을 때, 다음 일정이 존재하지 않으면 예외가 발생한다.")
    @Test
    void findUpcomingEvent_ifEventNotFound() {
        // given
        final Meeting meeting = dataSupport.saveMeeting(MORAGORA.create());

        final Event event1 = EVENT1.create(meeting);
        final Event event2 = EVENT2.create(meeting);
        dataSupport.saveEvent(event1);
        dataSupport.saveEvent(event2);

        final LocalDateTime dateTime = LocalDateTime.of(2022, 8, 3, 10, 6);
        serverTimeManager.refresh(dateTime);

        // when, then
        assertThatThrownBy(() -> eventService.findUpcomingEvent(meeting.getId()))
                .isInstanceOf(EventNotFoundException.class);
    }
}
