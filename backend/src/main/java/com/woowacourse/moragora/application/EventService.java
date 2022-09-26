package com.woowacourse.moragora.application;

import com.woowacourse.moragora.domain.attendance.Attendance;
import com.woowacourse.moragora.domain.attendance.AttendanceRepository;
import com.woowacourse.moragora.domain.attendance.Status;
import com.woowacourse.moragora.domain.event.Event;
import com.woowacourse.moragora.domain.event.EventRepository;
import com.woowacourse.moragora.domain.meeting.Meeting;
import com.woowacourse.moragora.domain.meeting.MeetingRepository;
import com.woowacourse.moragora.domain.participant.Participant;
import com.woowacourse.moragora.dto.request.event.EventCancelRequest;
import com.woowacourse.moragora.dto.request.event.EventRequest;
import com.woowacourse.moragora.dto.request.event.EventsRequest;
import com.woowacourse.moragora.dto.response.event.EventResponse;
import com.woowacourse.moragora.dto.response.event.EventsResponse;
import com.woowacourse.moragora.exception.ClientRuntimeException;
import com.woowacourse.moragora.exception.event.EventNotFoundException;
import com.woowacourse.moragora.exception.meeting.MeetingNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final ScheduledTasks scheduledTasks;
    private final TaskScheduler taskScheduler;
    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;
    private final AttendanceRepository attendanceRepository;
    private final ServerTimeManager serverTimeManager;
    private final Scheduler scheduler;

    public EventService(final ScheduledTasks scheduledTasks,
                        final TaskScheduler taskScheduler,
                        final EventRepository eventRepository,
                        final MeetingRepository meetingRepository,
                        final AttendanceRepository attendanceRepository,
                        final ServerTimeManager serverTimeManager,
                        final Scheduler scheduler) {
        this.scheduledTasks = scheduledTasks;
        this.taskScheduler = taskScheduler;
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
        this.attendanceRepository = attendanceRepository;
        this.serverTimeManager = serverTimeManager;
        this.scheduler = scheduler;
    }

    @Transactional
    public void save(final EventsRequest request, final Long meetingId) {
        final Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);
        final List<Event> requestEvents = request.toEntities(meeting);

        validateEventDateNotPast(requestEvents);
        validateDuplicatedEventDate(requestEvents);
        validateAttendanceStartTimeIsAfterNow(requestEvents);

        final List<Event> existingEvents = findExistingEvents(request, meetingId);
        final List<Event> newEvents = findEventsToSave(requestEvents, existingEvents);

        updateEvent(requestEvents, existingEvents);
        final List<Event> savedEvents = eventRepository.saveAll(newEvents);
        saveAllAttendances(meeting.getParticipants(), savedEvents);

        existingEvents.addAll(savedEvents);
        scheduleAttendancesUpdateByEvents(existingEvents);
    }

    private List<Event> findExistingEvents(final EventsRequest request, final Long meetingId) {
        final List<LocalDate> datesToSearch = request.getEvents().stream()
                .map(EventRequest::getDate).
                collect(Collectors.toList());
        return eventRepository.findByMeetingIdAndDateIn(meetingId, datesToSearch);
    }

    private List<Event> findEventsToSave(final List<Event> requestEvents, final List<Event> existingEvents) {
        return requestEvents.stream()
                .filter(it -> !existingEvents.stream()
                        .map(Event::getDate)
                        .collect(Collectors.toList()).contains(it.getDate()))
                .collect(Collectors.toList());
    }

    private void updateEvent(final List<Event> requestEvents, final List<Event> existingEvents) {
        for (Event existingEvent : existingEvents) {
            final Optional<Event> searchedEvent = requestEvents.stream()
                    .filter(it -> it.isSameDate(existingEvent))
                    .findAny();
            searchedEvent.ifPresent(existingEvent::updateTime);
        }
    }

    public void scheduleAttendancesUpdateByEvents(final List<Event> newEvents) {
        newEvents.forEach(event -> {
            final ScheduledFuture<?> schedule = taskScheduler.schedule(
                    () -> scheduler.updateAttendancesToTardyAfterClosedTime(event),
                    calculateUpdateInstant(event));
            scheduledTasks.put(event, schedule);
        });
    }

    @Transactional
    public void cancel(final EventCancelRequest request, final Long meetingId) {
        meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);
        final List<LocalDate> dates = request.getDates();
        List<Event> events = eventRepository.findByMeetingIdAndDateIn(meetingId, dates);
        final List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        attendanceRepository.deleteByEventIdIn(eventIds);
        eventRepository.deleteByIdIn(eventIds);
        events.forEach(scheduledTasks::remove);
    }

    public EventResponse findUpcomingEvent(final Long meetingId) {
        final Event event = eventRepository.findFirstByMeetingIdAndDateGreaterThanEqualOrderByDate(
                        meetingId, serverTimeManager.getDate())
                .orElseThrow(EventNotFoundException::new);

        final LocalTime entranceTime = event.getStartTime();
        final LocalTime attendanceOpenTime = serverTimeManager.calculateOpenTime(entranceTime);
        final LocalTime attendanceClosedTime = serverTimeManager.calculateAttendanceCloseTime(entranceTime);
        return EventResponse.of(event, attendanceOpenTime, attendanceClosedTime);
    }

    public EventsResponse findByDuration(final Long meetingId, final LocalDate begin, final LocalDate end) {
        validateBeginIsGreaterThanEqualEnd(begin, end);
        List<Event> events = eventRepository.findByMeetingIdAndDuration(meetingId, begin, end);
        final List<EventResponse> eventResponses = events.stream()
                .map(event -> EventResponse.of(event, serverTimeManager.calculateOpenTime(event.getStartTime()),
                        serverTimeManager.calculateAttendanceCloseTime(event.getStartTime()))
                )
                .collect(Collectors.toList());

        return new EventsResponse(eventResponses);
    }

    private void saveAllAttendances(final List<Participant> participants, final List<Event> events) {
        final List<Attendance> attendances = events.stream()
                .map(event -> createAttendances(participants, event))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        attendanceRepository.saveAll(attendances);
    }

    private List<Attendance> createAttendances(final List<Participant> participants, final Event event) {
        return participants.stream()
                .map(participant -> new Attendance(Status.NONE, false, participant, event))
                .collect(Collectors.toList());
    }

    private Instant calculateUpdateInstant(final Event event) {
        final LocalTime startTime = event.getStartTime();
        final LocalTime closingTime = serverTimeManager.calculateAttendanceCloseTime(startTime);
        return closingTime.atDate(event.getDate()).
                atZone(ZoneId.systemDefault()).toInstant();
    }

    private void validateBeginIsGreaterThanEqualEnd(final LocalDate begin, final LocalDate end) {
        if (begin == null || end == null) {
            return;
        }

        if (begin.isAfter(end)) {
            throw new ClientRuntimeException("기간의 입력이 잘못되었습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateEventDateNotPast(final List<Event> events) {
        final boolean exists = events.stream()
                .anyMatch(event -> event.isDateBefore(serverTimeManager.getDate()));
        if (exists) {
            throw new ClientRuntimeException("오늘 이전의 이벤트를 생성할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDuplicatedEventDate(final List<Event> events) {
        final List<LocalDate> dates = events.stream()
                .map(Event::getDate)
                .distinct()
                .collect(Collectors.toList());
        if (dates.size() != events.size()) {
            throw new ClientRuntimeException("하루에 복수의 일정을 생성할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateAttendanceStartTimeIsAfterNow(final List<Event> events) {
        final Optional<Event> event = events.stream()
                .filter(it -> it.isSameDate(serverTimeManager.getDate()))
                .findAny();

        event.ifPresent(it -> {
            if (serverTimeManager.isAfter(it.getStartTime())) {
                throw new ClientRuntimeException("과거의 일정을 생성할 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
        });
    }

    public Map<Event, ScheduledFuture<?>> getScheduledTasks() {
        return scheduledTasks.getValues();
    }
}
