package com.woowacourse.moragora.repository;

import com.woowacourse.moragora.entity.Participant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ParticipantRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public ParticipantRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public Participant save(final Participant participant) {
        entityManager.persist(participant);
        return participant;
    }

    public List<Participant> findByMeetingId(final Long meetingId) {
        return entityManager.createQuery("select p from Participant p where p.meeting.id = :meetingId",
                        Participant.class)
                .setParameter("meetingId", meetingId)
                .getResultList();
    }
}
