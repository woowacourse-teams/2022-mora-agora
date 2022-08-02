package com.woowacourse.moragora.support;

import com.woowacourse.moragora.entity.Attendance;
import com.woowacourse.moragora.entity.Meeting;
import com.woowacourse.moragora.entity.Participant;
import com.woowacourse.moragora.entity.Status;
import com.woowacourse.moragora.entity.user.User;
import com.woowacourse.moragora.repository.AttendanceRepository;
import com.woowacourse.moragora.repository.MeetingRepository;
import com.woowacourse.moragora.repository.ParticipantRepository;
import com.woowacourse.moragora.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DataSupport {

    private final UserRepository userRepository;

    private final MeetingRepository meetingRepository;

    private final ParticipantRepository participantRepository;

    private final AttendanceRepository attendanceRepository;

    public DataSupport(final UserRepository userRepository,
                       final MeetingRepository meetingRepository,
                       final ParticipantRepository participantRepository,
                       final AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public Participant saveParticipant(final User user, final Meeting meeting) {
        final User savedUser = userRepository.save(user);
        final Meeting savedMeeting = meetingRepository.save(meeting);
        final Participant participant = participantRepository.save(new Participant(savedUser, savedMeeting));
        participant.mapMeeting(savedMeeting);
        return participant;
    }

    public Attendance saveAttendance(final Participant participant, final LocalDate attendanceDate, final
    Status status) {
        return attendanceRepository.save(new Attendance(participant, attendanceDate, status));
    }

    public User saveUser(final User user) {
        return userRepository.save(user);
    }

    public List<Long> saveUsers(final List<User> users) {
        for (User user : users) {
            userRepository.save(user);
        }

        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    public Meeting saveMeeting(final Meeting meeting) {
        return meetingRepository.save(meeting);
    }
}
