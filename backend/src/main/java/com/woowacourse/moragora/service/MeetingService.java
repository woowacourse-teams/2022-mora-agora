package com.woowacourse.moragora.service;

import com.woowacourse.moragora.dto.MeetingRequest;
import com.woowacourse.moragora.dto.MeetingResponse;
import com.woowacourse.moragora.dto.MyMeetingsResponse;
import com.woowacourse.moragora.dto.UserAttendanceRequest;
import com.woowacourse.moragora.dto.UserResponse;
import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.exception.AttendanceNotFoundException;
import com.woowacourse.moragora.exception.ClosingTimeExcessException;
import com.woowacourse.moragora.exception.IllegalParticipantException;
import com.woowacourse.moragora.exception.MeetingNotFoundException;
import com.woowacourse.moragora.exception.ParticipantNotFoundException;
import com.woowacourse.moragora.exception.UserNotFoundException;
import com.woowacourse.moragora.repository.AttendanceRepository;
import com.woowacourse.moragora.repository.MeetingRepository;
import com.woowacourse.moragora.repository.ParticipantRepository;
import com.woowacourse.moragora.repository.UserRepository;
import com.woowacourse.moragora.service.closingstrategy.TimeChecker;
import com.woowacourse.moragora.util.CurrentDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private static final String USER_IDS_DUPLICATION_ERROR_MESSAGE = "참가자 명단에 중복이 있습니다.";
    private static final String USER_IDS_CONTAIN_LOGIN_ID_ERROR_MESSAGE = "생성자가 참가자 명단에 포함되어 있습니다.";
    private static final String EMPTY_USER_IDS_ERROR_MESSAGE = "생성자를 제외한 참가자가 없습니다.";

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final TimeChecker timeChecker;
    private final CurrentDateTime currentDateTime;

    public MeetingService(final MeetingRepository meetingRepository,
                          final ParticipantRepository participantRepository,
                          final AttendanceRepository attendanceRepository,
                          final UserRepository userRepository,
                          final TimeChecker timeChecker, final CurrentDateTime currentDateTime) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.timeChecker = timeChecker;
        this.currentDateTime = currentDateTime;
    }

    // TODO: for 문 안에 insert 한번에 날리는 것 고려
    // TODO: master 지정해야함
    @Transactional
    public Long save(final MeetingRequest request, final Long loginId) {
        final Meeting meeting = meetingRepository.save(request.toEntity());

        final List<Long> userIds = request.getUserIds();
        validateUserIds(userIds, loginId);

        final User loginUser = findUser(loginId);
        final List<User> users = userRepository.findByIds(userIds);
        validateUserExists(userIds, users);

        final Participant loginParticipant = new Participant(loginUser, meeting);
        final List<Participant> participants = users.stream()
                .map(user -> new Participant(user, meeting))
                .collect(Collectors.toList());
        participants.add(loginParticipant);

        for (Participant participant : participants) {
            participantRepository.save(participant);
        }

        return meeting.getId();
    }

    // TODO 1 + N 최적화 대상
    @Transactional
    public MeetingResponse findById(final Long meetingId) {
        final Meeting meeting = findMeeting(meetingId);
        final List<Participant> participants = participantRepository.findByMeetingId(meeting.getId());

        putAttendanceIfAbsent(participants);

        final List<UserResponse> userResponses = participants.stream()
                .map(participant -> UserResponse.of(participant.getUser(), getTardyCount(participant)))
                .collect(Collectors.toUnmodifiableList());

        return MeetingResponse.of(meeting, userResponses, getMeetingAttendanceCount(participants.get(0)));
    }

    public MyMeetingsResponse findAllByUserId(final Long userId) {
        final LocalDateTime now = LocalDateTime.now();
        final List<Participant> participants = participantRepository.findByUserId(userId);
        final List<Meeting> meetings = participants.stream()
                .map(Participant::getMeeting)
                .collect(Collectors.toList());

        return MyMeetingsResponse.of(now, timeChecker, meetings);
    }


    // TODO update (1 + N) -> 최적하기
    // TODO 출석 제출할 때 구현 예정
    @Transactional
    public void updateAttendance(final Long meetingId,
                                 final Long userId,
                                 final UserAttendanceRequest request,
                                 final Long loginId) {
        LocalDateTime nowDateTime = currentDateTime.getValue();

        final Meeting meeting = findMeeting(meetingId);
        final User user = findUser(userId);

        validateAttendanceTime(meeting);

        final Participant participant = participantRepository.findByMeetingIdAndUserId(meeting.getId(), user.getId())
                .orElseThrow(ParticipantNotFoundException::new);

        final Attendance attendance = attendanceRepository
                .findByParticipantIdAndAttendanceDate(participant.getId(), nowDateTime.toLocalDate())
                .orElseThrow(AttendanceNotFoundException::new);

        attendance.changeAttendanceStatus(request.getAttendanceStatus());
    }

    private Meeting findMeeting(final Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(MeetingNotFoundException::new);
    }

    private User findUser(final Long id) {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * 참가자 userIds 내부에 loginId가 있는지 검증해야 userIds.size()가 0인지 검증이 정상적으로 이루어집니다.
     */
    private void validateUserIds(final List<Long> userIds, final Long loginId) {
        if (Set.copyOf(userIds).size() != userIds.size()) {
            throw new IllegalParticipantException(USER_IDS_DUPLICATION_ERROR_MESSAGE);
        }

        if (userIds.contains(loginId)) {
            throw new IllegalParticipantException(USER_IDS_CONTAIN_LOGIN_ID_ERROR_MESSAGE);
        }

        if (userIds.size() == 0) {
            throw new IllegalParticipantException(EMPTY_USER_IDS_ERROR_MESSAGE);
        }
    }

    private void validateUserExists(final List<Long> userIds, final List<User> users) {
        if (users.size() != userIds.size()) {
            throw new UserNotFoundException();
        }
    }

    private int getTardyCount(final Participant participant) {
        final List<Attendance> attendances = attendanceRepository.findByParticipantId(participant.getId());
        return (int) attendances.stream()
                .filter(attendance -> attendance.isSameStatus(Status.TARDY))
                .count();
    }

    private void validateAttendanceTime(final Meeting meeting) {
        final LocalTime entranceTime = meeting.getEntranceTime();
        final boolean isOver = timeChecker.isExcessClosingTime(entranceTime);
        if (isOver) {
            throw new ClosingTimeExcessException();
        }
    }

    private void putAttendanceIfAbsent(final List<Participant> participants) {
        final List<Long> participantIds = participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());
        final LocalDate today = currentDateTime.getValue().toLocalDate();
        final List<Attendance> attendances =
                attendanceRepository.findByParticipantIdsAndAttendanceDate(participantIds, today);

        if (attendances.size() == 0) {
            saveAttendances(participants, today);
        }
    }

    private void saveAttendances(final List<Participant> participants, final LocalDate today) {
        for (final Participant participant : participants) {
            attendanceRepository.save(new Attendance(participant, today, Status.TARDY));
        }
    }

    private long getMeetingAttendanceCount(final Participant anyParticipant) {
        return attendanceRepository.findAttendanceCountById(anyParticipant.getId());
    }
}
