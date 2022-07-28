package com.woowacourse.moragora.service;

import com.woowacourse.moragora.dto.MeetingRequest;
import com.woowacourse.moragora.dto.MeetingResponse;
import com.woowacourse.moragora.dto.MyMeetingResponse;
import com.woowacourse.moragora.dto.MyMeetingsResponse;
import com.woowacourse.moragora.dto.ParticipantResponse;
import com.woowacourse.moragora.entity.AllAttendances;
import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Attendances;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.exception.meeting.MeetingNotFoundException;
import com.woowacourse.moragora.exception.participant.InvalidParticipantException;
import com.woowacourse.moragora.exception.user.UserNotFoundException;
import com.woowacourse.moragora.repository.AttendanceRepository;
import com.woowacourse.moragora.repository.MeetingRepository;
import com.woowacourse.moragora.repository.ParticipantRepository;
import com.woowacourse.moragora.repository.UserRepository;
import com.woowacourse.moragora.support.ServerTimeManager;
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

    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final ServerTimeManager serverTimeManager;

    public MeetingService(final MeetingRepository meetingRepository,
                          final ParticipantRepository participantRepository,
                          final AttendanceRepository attendanceRepository,
                          final UserRepository userRepository,
                          final ServerTimeManager serverTimeManager) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.serverTimeManager = serverTimeManager;
    }

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

        for (final Participant participant : participants) {
            participant.mapMeeting(meeting);
            participantRepository.save(participant);
        }

        return meeting.getId();
    }

    @Transactional
    public MeetingResponse findById(final Long meetingId) {
        final Meeting meeting = findMeeting(meetingId);
        final List<Participant> participants = meeting.getParticipants();

        putAttendanceIfAbsent(participants);

        final AllAttendances allAttendances = extractAllAttendances(participants);
        final boolean isOver = serverTimeManager.isOverClosingTime(meeting.getEntranceTime());
        final List<ParticipantResponse> participantResponses = participants.stream()
                .map(participant -> generateParticipantResponse(serverTimeManager.getDateAndTime(),
                        allAttendances, isOver, participant))
                .collect(Collectors.toList());

        return MeetingResponse.of(meeting, participantResponses, allAttendances.extractProceedDate());
    }


    public MyMeetingsResponse findAllByUserId(final Long userId) {
        final List<Participant> participants = participantRepository.findByUserId(userId);

        final List<MyMeetingResponse> myMeetingResponses = participants.stream()
                .map(this::generateMyMeetingResponse)
                .collect(Collectors.toList());

        return MyMeetingsResponse.of(serverTimeManager.getDateAndTime(), myMeetingResponses);
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

        if (userIds.size() == 0) {
            throw new InvalidParticipantException("생성자를 제외한 참가자가 없습니다.");
        }
    }

    private void validateUserExists(final List<Long> userIds, final List<User> users) {
        if (users.size() != userIds.size()) {
            throw new UserNotFoundException();
        }
    }

    private void saveAttendances(final List<Participant> participants, final LocalDate today) {
        for (final Participant participant : participants) {
            attendanceRepository.save(new Attendance(participant, today, Status.TARDY));
        }
    }

    private AllAttendances extractAllAttendances(final List<Participant> participants) {
        final List<Long> participantIds = participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());
        final List<Attendance> foundAttendances = attendanceRepository.findByParticipantIds(participantIds);
        return new AllAttendances(foundAttendances);
    }

    private ParticipantResponse generateParticipantResponse(final LocalDateTime now,
                                                            final AllAttendances allAttendances,
                                                            final boolean isOver,
                                                            final Participant participant) {
        final Attendances attendances = allAttendances.extractAttendancesByParticipant(participant);
        final Status status = attendances.extractAttendanceByDate(now.toLocalDate()).getStatus();
        final int tardyCount = attendances.countTardy(isOver, now.toLocalDate());

        return ParticipantResponse.of(participant.getUser(), status, tardyCount);
    }

    private MyMeetingResponse generateMyMeetingResponse(final Participant participant) {
        final Meeting meeting = participant.getMeeting();
        final List<Participant> participants = meeting.getParticipants();
        final AllAttendances allAttendances = extractAllAttendances(participants);
        final Attendances attendances = allAttendances.extractAttendancesByParticipant(participant);

        final boolean isActive = serverTimeManager.isAttendanceTime(meeting.getEntranceTime());
        final boolean isOver = serverTimeManager.isOverClosingTime(meeting.getEntranceTime());
        final LocalTime closingTime = serverTimeManager.calculateClosingTime(meeting.getEntranceTime());
        final int tardyCount = attendances.countTardy(isOver, serverTimeManager.getDate());

        return MyMeetingResponse.of(meeting, isActive, closingTime, tardyCount);
    }

    private void putAttendanceIfAbsent(final List<Participant> participants) {
        final List<Long> participantIds = participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());
        final List<Attendance> attendances =
                attendanceRepository.findByParticipantIdsAndAttendanceDate(participantIds, serverTimeManager.getDate());

        if (attendances.size() == 0) {
            saveAttendances(participants, serverTimeManager.getDate());
        }
    }

    private Meeting findMeeting(final Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(MeetingNotFoundException::new);
    }

    private User findUser(final Long id) {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }
}
