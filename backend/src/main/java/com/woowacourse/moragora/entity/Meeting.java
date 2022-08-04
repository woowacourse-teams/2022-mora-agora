package com.woowacourse.moragora.entity;

import com.woowacourse.moragora.exception.meeting.IllegalStartEndDateException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting")
@NoArgsConstructor
@Getter
public class Meeting {

    @OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
    private final List<Participant> participants = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;

    public Meeting(final String name,
                   final LocalDate startDate,
                   final LocalDate endDate) {
        validateStartEndDate(startDate, endDate);
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private void validateStartEndDate(final LocalDate startDate, final LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalStartEndDateException();
        }
    }

    public List<Long> getParticipantIds() {
        return participants.stream()
                .map(Participant::getId)
                .collect(Collectors.toList());
    }
}
