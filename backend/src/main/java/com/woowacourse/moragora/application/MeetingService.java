package com.woowacourse.moragora.application;

import com.woowacourse.moragora.domain.attendance.Attendance;
import com.woowacourse.moragora.domain.attendance.AttendanceRepository;
import com.woowacourse.moragora.domain.attendance.MeetingAttendances;
import com.woowacourse.moragora.domain.attendance.ParticipantAttendances;
import com.woowacourse.moragora.domain.event.Event;
import com.woowacourse.moragora.domain.event.EventRepository;
import com.woowacourse.moragora.domain.meeting.Meeting;
import com.woowacourse.moragora.domain.meeting.MeetingRepository;
import com.woowacourse.moragora.domain.participant.Participant;
import com.woowacourse.moragora.domain.participant.ParticipantAndCount;
import com.woowacourse.moragora.domain.participant.ParticipantRepository;
import com.woowacourse.moragora.domain.query.QueryRepository;
import com.woowacourse.moragora.domain.user.User;
import com.woowacourse.moragora.domain.user.UserRepository;
import com.woowacourse.moragora.dto.request.meeting.MasterRequest;
import com.woowacourse.moragora.dto.request.meeting.MeetingRequest;
import com.woowacourse.moragora.dto.request.meeting.MeetingUpdateRequest;
import com.woowacourse.moragora.dto.response.event.EventResponse;
import com.woowacourse.moragora.dto.response.meeting.MeetingResponse;
import com.woowacourse.moragora.dto.response.meeting.MyMeetingResponse;
import com.woowacourse.moragora.dto.response.meeting.MyMeetingsResponse;
import com.woowacourse.moragora.exception.ClientRuntimeException;
import com.woowacourse.moragora.exception.meeting.MeetingNotFoundException;
import com.woowacourse.moragora.exception.participant.InvalidParticipantException;
import com.woowacourse.moragora.exception.participant.ParticipantNotFoundException;
import com.woowacourse.moragora.exception.user.UserNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final QueryRepository queryRepository;
    private final ServerTimeManager serverTimeManager;

    public MeetingService(final MeetingRepository meetingRepository,
                          final EventRepository eventRepository,
                          final ParticipantRepository participantRepository,
                          final AttendanceRepository attendanceRepository,
                          final UserRepository userRepository,
                          final QueryRepository queryRepository,
                          final ServerTimeManager serverTimeManager) {
        this.meetingRepository = meetingRepository;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.queryRepository = queryRepository;
        this.serverTimeManager = serverTimeManager;
    }

    @Transactional
    public Long save(final MeetingRequest request, final Long loginId) {
        final Meeting meeting = meetingRepository.save(request.toEntity());
        final List<Long> userIds = request.getUserIds();
        validateUserIds(userIds, loginId);

        final User loginUser = userRepository.findById(loginId)
                .orElseThrow(UserNotFoundException::new);
        final List<User> users = userRepository.findByIdIn(userIds);
        validateUsersExists(userIds, users);

        saveParticipants(meeting, loginUser, users);

        return meeting.getId();
    }

    public MeetingResponse findById(final Long meetingId, final Long loginId) {
        final LocalDate today = serverTimeManager.getDate();
        final Meeting meeting = meetingRepository.findMeetingAndParticipantsById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);

        final List<ParticipantAndCount> participantAndCounts = queryRepository
                .countParticipantsTardy(meeting.getParticipants(), today);
        meeting.allocateParticipantsTardyCount(participantAndCounts);

        final long attendedEventCount = eventRepository.countByMeetingIdAndDateLessThanEqual(meetingId, today);
        final Participant loginParticipant = meeting.findParticipant(loginId)
                .orElseThrow(ParticipantNotFoundException::new);

        return MeetingResponse.of(meeting, attendedEventCount, loginParticipant);
    }

    public MyMeetingsResponse findAllByUserId(final Long userId) {
        final List<Participant> participants = participantRepository.findByUserId(userId);

        final List<MyMeetingResponse> myMeetingResponses = participants.stream()
                .map(participant -> generateMyMeetingResponse(participant, serverTimeManager.getDate()))
                .collect(Collectors.toList());

        return new MyMeetingsResponse(myMeetingResponses);
    }

    @Transactional
    public void assignMaster(final Long meetingId, final MasterRequest request, final Long loginId) {
        final Long assignedUserId = request.getUserId();
        validateMeetingExists(meetingId);
        validateUserExists(assignedUserId);
        validateAssignee(loginId, assignedUserId);

        final Participant assignedParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, assignedUserId)
                .orElseThrow(ParticipantNotFoundException::new);
        final Participant masterParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, loginId)
                .orElseThrow(ParticipantNotFoundException::new);

        assignedParticipant.updateIsMaster(true);
        masterParticipant.updateIsMaster(false);
    }

    @Transactional
    public void updateName(final MeetingUpdateRequest request, final Long meetingId) {
        final Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);
        meeting.updateName(request.getName());
    }

    @Transactional
    public void deleteParticipant(final long meetingId, final long userId) {
        validateMeetingExists(meetingId);
        validateUserExists(userId);

        final Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(ParticipantNotFoundException::new);
        validateNotMaster(participant);

        attendanceRepository.deleteByParticipantId(participant.getId());
        participantRepository.delete(participant);
    }

    @Transactional
    public void deleteMeeting(final Long meetingId) {
        final Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);

        final List<Long> participantIds = meeting.getParticipantIds();

        attendanceRepository.deleteByParticipantIdIn(participantIds);
        participantRepository.deleteByIdIn(participantIds);
        eventRepository.deleteByMeetingId(meeting.getId());
        meetingRepository.deleteById(meeting.getId());
    }

    /**
     * 참가자 userIds 내부에 loginId가 있는지 검증해야 userIds.size()가 0인지 검증이 정상적으로 이루어집니다.
     */
    private void validateUserIds(final List<Long> userIds, final Long loginId) {
        if (Set.copyOf(userIds).size() != userIds.size()) {
            throw new InvalidParticipantException("참가자 명단에 중복이 있습니다.");
        }

        if (userIds.contains(loginId)) {
            throw new InvalidParticipantException("생성자가 참가자 명단에 포함되어 있습니다.");
        }

        if (userIds.isEmpty()) {
            throw new InvalidParticipantException("생성자를 제외한 참가자가 없습니다.");
        }
    }

    private void validateUsersExists(final List<Long> userIds, final List<User> users) {
        if (users.size() != userIds.size()) {
            throw new UserNotFoundException();
        }
    }

    private void saveParticipants(final Meeting meeting, final User loginUser, final List<User> users) {
        final Participant loginParticipant = new Participant(loginUser, meeting, true);
        final List<Participant> participants = users.stream()
                .map(user -> new Participant(user, meeting, false))
                .collect(Collectors.toList());
        participants.add(loginParticipant);

        for (Participant participant : participants) {
            participant.mapMeeting(meeting);
            participantRepository.save(participant);
        }
    }

    private MeetingAttendances getMeetingAttendances(final Meeting meeting, final LocalDate today) {
        final List<Long> participantIds = meeting.getParticipantIds();
        final List<Attendance> foundAttendances = attendanceRepository
                .findByParticipantIdInAndEventDateLessThanEqual(participantIds, today);
        return new MeetingAttendances(foundAttendances, participantIds.size());
    }

    private MyMeetingResponse generateMyMeetingResponse(final Participant participant, final LocalDate today) {
        final Meeting meeting = participant.getMeeting();
        final boolean isLoginUserMaster = participant.getIsMaster();

        final MeetingAttendances meetingAttendances = getMeetingAttendances(meeting, today);
        final boolean isCoffeeTime = meetingAttendances.isTardyStackFull();
        final int tardyCount = countTardyByParticipant(participant, meetingAttendances);

        final Optional<Event> upcomingEvent = eventRepository
                .findFirstByMeetingIdAndDateGreaterThanEqualOrderByDate(meeting.getId(), today);
        if (upcomingEvent.isEmpty()) {
            return MyMeetingResponse.of(
                    meeting, tardyCount, isLoginUserMaster, isCoffeeTime, false, null
            );
        }
        final Event event = upcomingEvent.get();
        final LocalTime startTime = event.getStartTime();
        final boolean isActive = event.isSameDate(today) && serverTimeManager.isAttendanceOpen(startTime);
        final LocalTime attendanceOpenTime = serverTimeManager.calculateOpenTime(startTime);
        final LocalTime attendanceClosedTime = serverTimeManager.calculateAttendanceCloseTime(startTime);
        return MyMeetingResponse.of(
                meeting, tardyCount, isLoginUserMaster, isCoffeeTime, isActive,
                EventResponse.of(event, attendanceOpenTime, attendanceClosedTime)
        );
    }

    private int countTardyByParticipant(final Participant participant, final MeetingAttendances meetingAttendances) {
        final ParticipantAttendances participantAttendances = meetingAttendances
                .extractAttendancesByParticipant(participant);
        return participantAttendances.countTardy();
    }

    private void validateAssignee(final Long loginId, final Long participantId) {
        if (Objects.equals(loginId, participantId)) {
            throw new ClientRuntimeException("스스로에게 마스터 권한을 넘길 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateNotMaster(final Participant participant) {
        if (Boolean.TRUE.equals(participant.getIsMaster())) {
            throw new ClientRuntimeException("마스터는 모임을 나갈 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    private void validateMeetingExists(final Long meetingId) {
        if (!meetingRepository.existsById(meetingId)) {
            throw new MeetingNotFoundException();
        }
    }
    private void validateUserExists(final Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }
}
