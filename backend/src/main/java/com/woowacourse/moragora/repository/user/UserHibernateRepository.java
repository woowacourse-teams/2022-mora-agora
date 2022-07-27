package com.woowacourse.moragora.repository.user;

import com.woowacourse.moragora.entity.user.User;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class UserHibernateRepository implements UserRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public UserHibernateRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public User save(final User user) {
        entityManager.persist(user);
        return user;
    }

    // TODO 메서드명 수정
    public List<User> findByIdIn(final List<Long> ids) {
        return entityManager.createQuery("select u from User u where u.id in :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public Optional<User> findByEmail(final String email) {
        try {
            User user = entityManager.createQuery("select u from User u where u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(final Long id) {
        final User user = entityManager.find(User.class, id);
        return Optional.ofNullable(user);
    }

    public List<User> findByNicknameContainingOrEmailContaining(final String keyword) {
        return entityManager.createQuery(
                "select u from User u where u.email like :keyword or u.nickname like :keyword", User.class)
                .setParameter("keyword", "%" + keyword + "%")
                .getResultList();
    }
}
