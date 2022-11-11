package com.woowacourse.moragora.domain.participant;

import com.woowacourse.moragora.domain.exception.BusinessException;
import com.woowacourse.moragora.domain.meeting.Meeting;
import com.woowacourse.moragora.domain.user.User;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    private Long id;

    @Column(columnDefinition = "boolean default false")
    private Boolean isMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Transient
    private Long tardyCount;

    public Participant(final User user, final Meeting meeting, final boolean isMaster) {
        this(null, user, meeting, isMaster);
    }

    @Builder
    public Participant(final Long id, final User user, final Meeting meeting, final boolean isMaster) {
        this.id = id;
        this.user = user;
        this.meeting = meeting;
        this.isMaster = isMaster;
    }

    public void mapMeeting(final Meeting meeting) {
        this.meeting = meeting;

        if (!meeting.getParticipants().contains(this)) {
            meeting.getParticipants().add(this);
        }
    }

    public void updateIsMaster(final boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isSameParticipant(final Long userId) {
        return user.isSameUser(userId);
    }

    public void allocateTardyCount(final Long tardyCount) {
        this.tardyCount = tardyCount;
    }

    public Long getTardyCount() {
        if (tardyCount == null) {
            throw new BusinessException("지각 횟수가 할당되지 않았습니다.");
        }

        return tardyCount;
    }
}
